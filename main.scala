package project_2

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.rdd._


object main{

  val seed = new java.util.Date().hashCode;
  val rand = new scala.util.Random(seed);

  class hash_function(numBuckets_in: Long) extends Serializable {  // a 2-universal hash family, numBuckets_in is the numer of buckets
    val p: Long = 2147483587;  // p is a prime around 2^31 so the computation will fit into 2^63 (Long)
    val a: Long = (rand.nextLong %(p-1)) + 1  // a is a random number is [1,p]
    val b: Long = (rand.nextLong % p) // b is a random number in [0,p]
    val numBuckets: Long = numBuckets_in

    def convert(s: String, ind: Int): Long = {
      if(ind==0)
        return 0;
      return (s(ind-1).toLong + 256 * (convert(s,ind-1))) % p;
    }

    def hash(s: String): Long = {
      return ((a * convert(s,s.length) + b) % p) % numBuckets;
    }

    def hash(t: Long): Long = {
      return ((a * t + b) % p) % numBuckets;
    }

    def zeroes(num: Long, remain: Long): Int =
    {
      if((num & 1) == 1 || remain==1)
        return 0;
      return 1+zeroes(num >> 1, remain >> 1);
    }

    def zeroes(num: Long): Int =        /*calculates #consecutive trialing zeroes  */
    {
      return zeroes(num, numBuckets)
    }
  }

  class four_universal_Radamacher_hash_function extends hash_function(2) {  // a 4-universal hash family, numBuckets_in is the numer of buckets
    override val a: Long = (rand.nextLong % p)   // a is a random number is [0,p]
    override val b: Long = (rand.nextLong % p) // b is a random number in [0,p]
    val c: Long = (rand.nextLong % p)   // c is a random number is [0,p]
    val d: Long = (rand.nextLong % p) // d is a random number in [0,p]

    override def hash(s: String): Long = {     /* returns +1 or -1 with prob. 1/2 */
      val t= convert(s,s.length)
      val t2 = t*t % p
      val t3 = t2*t % p
      return if ( ( ((a * t3 + b* t2 + c*t + b) % p) & 1) == 0 ) 1 else -1;
    }

    override def hash(t: Long): Long = {       /* returns +1 or -1 with prob. 1/2 */
      val t2 = t*t % p
      val t3 = t2*t % p
      return if( ( ((a * t3 + b* t2 + c*t + b) % p) & 1) == 0 ) 1 else -1;
    }
  }

  // Fixed BJKSTSketch for distinct elements counting
  class BJKSTSketch(val maxBucketSize: Int) extends Serializable {
    var bucket: Set[String] = Set.empty[String]
    var z: Int = 0
    
    def this() = {
      this(0)
    }
    
    // Add an element to the sketch
    def add(element: String, hashFunc: hash_function): BJKSTSketch = {
      val h = hashFunc.hash(element)
      val zeros = countTrailingZeros(h) // Count trailing zeros
      
      // Only add elements with enough trailing zeros
      if (zeros >= z) {
        bucket += element
        
        // If bucket is too large, increase z and filter elements
        if (bucket.size > maxBucketSize) {
          z += 1
          bucket = bucket.filter(e => countTrailingZeros(hashFunc.hash(e)) >= z)
        }
      }
      
      this
    }
    
    // Merge with another sketch
    def merge(other: BJKSTSketch, hashFunc: hash_function): BJKSTSketch = {
      val result = new BJKSTSketch(maxBucketSize)
      result.z = math.max(this.z, other.z)
      
      // Combine buckets
      val combined = this.bucket ++ other.bucket
      
      // Filter elements with too few trailing zeros
      result.bucket = combined.filter(e => countTrailingZeros(hashFunc.hash(e)) >= result.z)
      
      // If bucket is still too large, increase z and filter again
      while (result.bucket.size > maxBucketSize) {
        result.z += 1
        result.bucket = result.bucket.filter(e => countTrailingZeros(hashFunc.hash(e)) >= result.z)
      }
      
      result
    }
    
    // Get the F0 estimate
    def estimate(): Double = {
      // Handle empty bucket case
      if (bucket.isEmpty) return 0.0
      
      // Basic formula: |B| * 2^z
      bucket.size * math.pow(2, z)
    }
    
    // Helper method to count trailing zeros
    private def countTrailingZeros(x: Long): Int = {
      // If x is 0, return 0 to avoid infinite trailing zeros
      if (x == 0) return 0
      
      // Count trailing zeros
      var value = x
      var count = 0
      while ((value & 1) == 0 && count < 64) {
        count += 1
        value >>= 1
      }
      
      count
    }
  }

  def BJKST(x: RDD[String], width: Int, trials: Int) : Double = {
    val estimates = for (i <- 0 until trials) yield {
      // Create a new hash function for this trial
      val hashFunc = new hash_function(Long.MaxValue)
      
      // Process the RDD
      val result = x.aggregate(new BJKSTSketch(width))(
        // Add each element to the sketch
        (sketch, element) => sketch.add(element, hashFunc),
        
        // Merge sketches from different partitions
        (sketch1, sketch2) => sketch1.merge(sketch2, hashFunc)
      )
      
      // Return the estimate for this trial
      result.estimate()
    }
    
    // Return the median of all trials
    val sortedEstimates = estimates.sorted.toArray
    if (trials % 2 == 0) {
      (sortedEstimates(trials/2 - 1) + sortedEstimates(trials/2)) / 2.0
    } else {
      sortedEstimates(trials/2)
    }
  }

  def tidemark(x: RDD[String], trials: Int): Double = {
    val h = Seq.fill(trials)(new hash_function(2000000000))

    def param0 = (accu1: Seq[Int], accu2: Seq[Int]) => Seq.range(0,trials).map(i => scala.math.max(accu1(i), accu2(i)))
    def param1 = (accu1: Seq[Int], s: String) => Seq.range(0,trials).map( i =>  scala.math.max(accu1(i), h(i).zeroes(h(i).hash(s))) )

    val x3 = x.aggregate(Seq.fill(trials)(0))( param1, param0)
    val ans = x3.map(z => scala.math.pow(2,0.5 + z)).sortWith(_ < _)( trials/2) /* Take the median of the trials */

    return ans
  }

  def Tug_of_War(x: RDD[String], width: Int, depth:Int) : Long = {
    // Create width * depth hash functions
    val hashFunctions = Array.fill(width * depth)(new four_universal_Radamacher_hash_function())
    
    // Compute all Tug-of-War sketches
    val sketches = hashFunctions.map { h =>
      x.map(s => h.hash(s)).sum()
    }
    
    // Group sketches into groups of size 'width'
    val groups = sketches.grouped(width).toArray
    
    // Compute the mean of squared values for each group
    val means = groups.map { group =>
      val squaredSum = group.map(x => x * x).sum
      squaredSum / width.toDouble
    }
    
    // Return the median of the depth means
    val sortedMeans = means.sorted
    val median = if (depth % 2 == 0) {
      (sortedMeans(depth / 2 - 1) + sortedMeans(depth / 2)) / 2
    } else {
      sortedMeans(depth / 2)
    }
    
    return median.toLong
  }

  def exact_F0(x: RDD[String]) : Long = {
    val ans = x.distinct.count
    return ans
  }

  def exact_F2(x: RDD[String]) : Long = {
    val frequencies = x.map(plate => (plate, 1L)).reduceByKey(_ + _)
    val squaredCounts = frequencies.map { case (_, count) => count * count }
    return squaredCounts.sum().toLong
  }

  def main(args: Array[String]) {
    val spark = SparkSession.builder().appName("Project_2").getOrCreate()

    if(args.length < 2) {
      println("Usage: project_2 input_path option = {BJKST, tidemark, ToW, exactF2, exactF0} ")
      sys.exit(1)
    }
    val input_path = args(0)

  //    val df = spark.read.format("csv").load("data/2014to2017.csv")
    val df = spark.read.format("csv").load(input_path)
    val dfrdd = df.rdd.map(row => row.getString(0))

    val startTimeMillis = System.currentTimeMillis()

    if(args(1)=="BJKST") {
      if (args.length != 4) {
        println("Usage: project_2 input_path BJKST #buckets trials")
        sys.exit(1)
      }
      val ans = BJKST(dfrdd, args(2).toInt, args(3).toInt)

      val endTimeMillis = System.currentTimeMillis()
      val durationSeconds = (endTimeMillis - startTimeMillis) / 1000

      println("==================================")
      println("BJKST Algorithm. Bucket Size:"+ args(2) + ". Trials:" + args(3) +". Time elapsed:" + durationSeconds + "s. Estimate: "+ans)
      println("==================================")
    }
    else if(args(1)=="tidemark") {
      if(args.length != 3) {
        println("Usage: project_2 input_path tidemark trials")
        sys.exit(1)
      }
      val ans = tidemark(dfrdd, args(2).toInt)
      val endTimeMillis = System.currentTimeMillis()
      val durationSeconds = (endTimeMillis - startTimeMillis) / 1000

      println("==================================")
      println("Tidemark Algorithm. Trials:" + args(2) + ". Time elapsed:" + durationSeconds + "s. Estimate: "+ans)
      println("==================================")

    }
    else if(args(1)=="ToW") {
       if(args.length != 4) {
         println("Usage: project_2 input_path ToW width depth")
         sys.exit(1)
      }
      val ans = Tug_of_War(dfrdd, args(2).toInt, args(3).toInt)
      val endTimeMillis = System.currentTimeMillis()
      val durationSeconds = (endTimeMillis - startTimeMillis) / 1000
      println("==================================")
      println("Tug-of-War F2 Approximation. Width :" +  args(2) + ". Depth: "+ args(3) + ". Time elapsed:" + durationSeconds + "s. Estimate: "+ans)
      println("==================================")
    }
    else if(args(1)=="exactF2") {
      if(args.length != 2) {
        println("Usage: project_2 input_path exactF2")
        sys.exit(1)
      }
      val ans = exact_F2(dfrdd)
      val endTimeMillis = System.currentTimeMillis()
      val durationSeconds = (endTimeMillis - startTimeMillis) / 1000

      println("==================================")
      println("Exact F2. Time elapsed:" + durationSeconds + "s. Estimate: "+ans)
      println("==================================")
    }
    else if(args(1)=="exactF0") {
      if(args.length != 2) {
        println("Usage: project_2 input_path exactF0")
        sys.exit(1)
      }
      val ans = exact_F0(dfrdd)
      val endTimeMillis = System.currentTimeMillis()
      val durationSeconds = (endTimeMillis - startTimeMillis) / 1000

      println("==================================")
      println("Exact F0. Time elapsed:" + durationSeconds + "s. Estimate: "+ans)
      println("==================================")
    }
    else {
      println("Usage: project_2 input_path option = {BJKST, tidemark, ToW, exactF2, exactF0} ")
      sys.exit(1)
    }

  }
}