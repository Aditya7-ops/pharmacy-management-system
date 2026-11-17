# Project Synopsis: Pharmacy Management System

## Title
Pharmacy Management System using Java Swing and MySQL

## Introduction
The Pharmacy Management System is a desktop application intended to help a small pharmacy maintain medicine stock and record sales. It uses Java Swing for the user interface and MySQL for storing data.

## Problem Statement
Maintaining medicine details and stock manually can lead to data-entry errors, difficulty finding medicines, and inaccurate stock records. A computerized system can make these tasks easier.

## Objectives
1. Store medicine details in a MySQL database.
2. Add, update, delete, and search medicine records.
3. Display current stock and prices.
4. Record medicine sales and update stock after a sale.
5. Demonstrate Java Swing, JDBC, and relational database concepts.

## Technology Used
- Programming language: Java
- GUI: Java Swing
- Database: MySQL
- Database connectivity: JDBC
- IDE: Eclipse

## Main Modules
1. Medicine Inventory: add, edit, delete, and view medicines.
2. Search: search medicines by name or category.
3. Sales: select a medicine, enter quantity, calculate total, record the sale, and reduce stock.
4. Database: persist medicine and sales information.

## Database Tables
- `medicines`: medicine identity, category, manufacturer, expiry date, quantity, and price.
- `sales`: medicine sold, quantity, unit price, total amount, and sale timestamp.

## Expected Outcome
The application will provide a simple desktop interface to manage medicine inventory and record sales with data saved in MySQL.

## Scope and Limitation
This is an educational project. It does not provide prescription validation, drug interaction checking, tax invoicing, supplier management, role-based login, or regulatory compliance features.
