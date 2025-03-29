# Large Scale Data Processing: Project 2
## Question 1: Exact F2

**On Local Machine**  
- **Time Elapsed:** 30s  
- **Estimate:** 8,567,966,130

**On GCP**  
- **Time Elapsed:** 95s  
- **Estimate:** 8,567,966,130

---

## Question 2: Tug-of-War F2 Approximation  
**Parameters:** Width = 10, Depth = 3

**On Local Machine**  
- **Time Elapsed:** 174s  
- **Estimate:** 9,270,022,870

**On GCP**  
- **Time Elapsed:** 587s  
- **Estimate:** 7,640,740,596

---

## Question 3: BJKST F0 Approximation  

**Exact F0:**  
- **Time Elapsed:** 29s  
- **Estimate:** 7,406,649  
- **Acceptable Range (+/-20%):** [5,925,319.2, 8,887,978.8]

After testing widths of 200/100/50..., the smallest width that achieves the error is **40**.

**BJKST Algorithm Parameters:** Bucket Size = 40, Trials = 5

**On Local Machine**  
- **Time Elapsed:** 33s  
- **Estimate:** 7,602,176.0

**On GCP**  
- **Time Elapsed:** 109s  
- **Estimate:** 7,340,032.0

---

## Question 4: Analysis

### BJKST vs Exact F0

The **BJKST algorithm** provides an excellent approximation of the number of distinct elements (F0) with a very small error margin of just **2.64%**. It achieves this accuracy with a compact bucket size of only **40**, making it very memory-efficient. Additionally, the time performance is impressive — only slightly slower than the exact calculation.

### Tug-of-War vs Exact F2

The **Tug-of-War algorithm** gives a good approximation of F2 (sum of squared frequencies) with an error of about **8.19%**. However, it is **significantly slower** than the exact computation, taking nearly **6 times longer** on the local machine and even more on GCP.
