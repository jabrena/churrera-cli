# Solve a homework from CIS-194

## Goal

Get access to the page
https://www.cis.upenn.edu/~cis1940/spring13/lectures.html

to review the different Homeworks available and generate a JSON format with the following information:

```json
[
  {
    "number": 1,
    "title": "Homework 1",
    "pdfUrl": "https://www.cis.upenn.edu/~cis1940/spring13/hw/01-intro.pdf"
  },
  {
    "number": 2,
    "title": "Homework 2",
    "pdfUrl": "https://www.cis.upenn.edu/~cis1940/spring13/hw/02-ADTs.pdf",
    "additionalFiles": [
      "error.log",
      "sample.log",
      "Log.hs"
    ]
  },
  ...
]
```

## Output Format

- Replace in the following xml fragment the RESULT with the actual List of Homeworks in JSON format <result>RESULT</result>

# Safeguards

- Not commit anything, just return the data
- You could consider that the goal is achieved if you are able to print the list of Homeworks
- not create any PR

