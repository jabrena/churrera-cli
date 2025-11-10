# Solve an Advent of code problem

## Role

You are a Senior software engineer with extensive experience in Java software development

## Goal

To solve the problem execute the next steps.

### Understand the whole project

Execute ./mvnw clean install to have all modules in local maven repository.

Review the whole repository to undersand the different maven modules
Put focus in the module common to understand how to desing the classes
Put foucs in the module third-party/aoc just in case you could use one of the classes to model your solutions

### Identify what problem to solve

Execute the command from the root pom.xml:

```bash
jbang aoc@jabrena pending
```

to return what years has pending problem to solve. It will return a list of years like: 2015 2016 2017 2018 2019 2020 2021 2022 2023

Take the first element of the results, a YEAR and with the year:

Verify that exist a module with the name of the YEAR.
and pass that YEAR to the following command:

```bash
jbang aoc@jabrena pending YEAR
```

that command will return a list of strings representing the days and parts pending to be solved.
From that result, get the FIRST value from the list which has the following schema:
9_2 which it means day=9 & part=2; day contains values from 1-31 & part could be 1 or 2.

if part is 1, review "Notes if part=1" in other case review "Notes if part=2"

### Notes if part=1

#### Create input data

In the module 2015, review if exist in src/test/resources the folder named dayX
in other case, create in src/test/resources create a folder named dayX where X is the day that you processed before

inside of that folder review if exist the file dayX-input.txt where X is the day
in other case, create a file named dayX-input.txt and put the content from:

```bash
jbang aoc@jabrena input 2015 X
```

#### Create a Unit Test

In the module 2015, review if exist in src/test/java/info/jab/aoc a previous package for dayX where X is the day
in other case, create the unit test, create a new package in 2015/src/test/java/info/jab/aoc named dayX where X is the day.

Inside of the new package create a Unit test to test the future Java class located in the same level:

2025/src/main/info/jab/aoc/dayX/DayX.java

Add a unit test for First part.

where X is the day.

In this phase following TDD, the test will pass the file name representing the input data to the future Java solution.

Review examples like: 2015/src/test/java/info/jab/aoc/day1/Day1Test.java

To understand the concept.

If you execute the tests with:

```bash
./mvnw clean verify
```
it will fail, but it is Ok, it is part of the TDD process.

Note: Not create any tests for the part 2.

####  Create a Java class with the solution

In the maven module 2015, in the package info.jab.aoc,
create a package with the name: day + day value, from the example 14, so "day14"

Over that package, implement a Java class with the name "DayX.java" where X is the day resolved previously.

That Java class will resolve the problem returned from the command:

```bash
jbang aoc@jabrena problem 2015 X
```

using the imput from:

```bash
jbang aoc@jabrena input 2015 X
```

note: change the value X with the day that you processed previously.

With the problem statement and the input, develop the solution in the Java class.

Note: Not create any solution for the part 2.

print the solution in the test and extract from that maven test execution the RESULT of the part 1.

use that RESULT to verify if the problem was resolved with the following command:

```bash
jbang aoc@jabrena submit 2015 1 1 RESULT
```

if the command returns: "❌ Wrong answer", you need to iterate the solution because it was not resolved.

Verify that everything works with

```
./mvnw clean verify
```

Verify that test load the input file
Verify that java solution take the input from the test
Verify that test has the right assert asserting the result
Verigy that the java solution doesn´t have any main method
Review that the input file has the right format: dayX-input.txt

If everything goes well, you achieved the goal.

### Notes if part=2

#### Create input data

In the module 2015, review if exist in src/test/resources the folder named dayX
in other case, create in src/test/resources create a folder named dayX where X is the day that you processed before

inside of that folder review if exist the file dayX-input.txt where X is the day
in other case, create a file named dayX-input.txt and put the content from:

```bash
jbang aoc@jabrena input 2015 X
```

#### Create a Unit Test

In the module 2015, review if exist in src/test/java/info/jab/aoc a previous package for dayX where X is the day
in other case, create the unit test, create a new package in 2015/src/test/java/info/jab/aoc named dayX where X is the day.

Inside of the new package create a Unit test to test the Java class located in the same level:

2025/src/main/info/jab/aoc/dayX/DayX.java

Add a unit test for Second part.

####  Create a Java class with the solution

Read the problem statement for the second part with:

```bash
jbang aoc@jabrena problem 2015 X
```

where X is the day and with the problem statement, create a solution which is compatible for both parts.

and verify that the solution using the unit test which print the RESULT. Verify the result if it is valid with:

```bash
jbang aoc@jabrena submit 2015 X 2 RESULT
```

Where X is the day

if the command returns: "❌ Wrong answer", you need to iterate the solution because it was not resolved.

If the solution is valid, update the unit tests with the right asserts and:

Verify that everything works with ./mvnw clean verify

Verify that test load the input file
Verify that java solution take the input from the test
Verify that test has the right assert asserting the result
Verify that the java solution doesn´t have any main method

# Safeguards

verify the changes only with `./mvnw clean verify`

In other case, you could consider that the goal is achieved and you can commit your java sources and create the PR.
Not commit any .class file
