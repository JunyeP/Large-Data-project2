# Large Scale Data Processing: Project 2

## Question 1: Exact F2

**On Local Machine**  
Exact F2. Time elapsed:30s. Estimate: 8567966130

**On GCP**  
Exact F2. Time elapsed:95s. Estimate: 8567966130

---

## Question 2: Tug-of-War F2 Approximation  
Width :10. Depth: 3

**On Local Machine**  
Tug-of-War F2 Approximation. Width :10. Depth: 3. Time elapsed:174s. Estimate: 9270022870

**On GCP**  
Tug-of-War F2 Approximation. Width :10. Depth: 3. Time elapsed:587s. Estimate: 7640740596

---

## Question 3: BJKST F0 Approximation  

Exact F0. Time elapsed:29s. Estimate: 7406649  
So the +/- 20% is [5925319.2, 8887978.8]  
After testing width of 200/100/50…, the smallest width that achieve the error is 40

**On Local Machine**  
BJKST Algorithm. Bucket Size:40. Trials:5. Time elapsed:33s. Estimate: 7602176.0

**On GCP**  
BJKST Algorithm. Bucket Size:40. Trials:5. Time elapsed:109s. Estimate: 7340032.0

---

## Question 4: Analysis

### BJKST vs Exact F0

The BJKST algorithm provides an excellent approximation of the number of distinct elements (F0) with a very small error margin of just 2.64%. The algorithm achieves this accuracy with a remarkably small bucket size of just 40, which is very memory-efficient. The time performance is also impressive - only slightly slower than the exact calculation.

### Tug-of-War vs Exact F2

The Tug-of-War algorithm provides a good approximation of F2 (sum of squared frequencies) with an error of about 8.19%. However, its runtime is significantly longer than the exact calculation, taking almost 6 times as long.
