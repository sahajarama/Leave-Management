Leave Management System

This is a console-based Leave Management System developed in Java. It provides a simple, command-line interface for employees to manage their leave applications and for administrators to oversee the process. The system connects to a MySQL database to store and retrieve all leave-related data.

Features
User Authentication: Supports separate login credentials for employees and administrators.

Employee Functionality:
Apply for different types of leave (Sick, Casual, Earned).
View personal leave history and current leave balances.

Administrator Functionality:
Approve or reject leave applications.
View all submitted leave requests.
Filter and view only pending leave requests.
Database Integration: Uses a MySQL database to maintain a persistent record of employees, their leave balances, and all leave applications.

Prerequisites
To run this application, you will need to have the following installed:
Java Development Kit (JDK): Version 8 or higher.
MySQL Database: A running instance of a MySQL server.

MySQL Connector/J: The JDBC driver for Java. You can download it from the official MySQL website or add it to your project's dependencies if you are using a build tool like Maven or Gradle.
