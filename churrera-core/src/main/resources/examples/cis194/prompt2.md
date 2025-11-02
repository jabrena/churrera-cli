# Solve a homework from CIS-194

## Goal

Get access to the page
https://www.cis.upenn.edu/~cis1940/spring13/lectures.html

to review the different Homeworks and check in the repository in
src/main/java/info/jab/cis194/

a new package named homework1 to homework12 exist in the repository.

if in the repository doesn´t exist a homeworkX, X represent the week, in that case take the minimum one which is not solved in the repository.
In other case, continue with the next Homework if exist a previous one.
If you are in Homework 9, jump to Homework 10 or higher.

Every homework has associated a Lecture, a Homework document and maybe some associated documents.

Example:

In the week 1,
exist a Lecture: https://www.cis.upenn.edu/~cis1940/spring13/lectures/01-intro.html
and a Homework: https://www.cis.upenn.edu/~cis1940/spring13/hw/01-intro.pdf

Process both documents, the first one to take the context about the ideas behind and later implement the Homework associated.

In the first Homework, the document in PDF format describe a set of Exercise
to be implemented in Java 25.

Understand the problem do a good analysis to implement the solution.
Apply TDD so, create in tests the new package:

src/test/java/info/jab/cis194/homework1

and Exercise by Exercise create the Test first:

src/test/java/info/jab/cis194/homework1/Exercice1Test.java

Once you have the initial test, execute:

./mvnw clean verify

and it will fail but this is something normal because you don´t have the implementation.

Implement the solution in:

src/main/java/info/jab/cis194/homework1/Exercice1.java

and execute:

./mvnw clean verify

This time it should pass the test, in order case, review the implementation

When one Exercise passes the tests, commit & Push to the PR and continue.

Follow this steps for all Exercises included in the Homework

# Safeguards

verify the changes only with `./mvnw clean verify`

In other case, you could consider that the goal is achieved and you can commit your java sources and create the PR.
Not commit any .class file
