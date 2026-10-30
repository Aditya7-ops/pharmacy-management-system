# Pharmacy Management System

A desktop application designed to streamline pharmacy operations, inventory tracking, and point-of-sale (POS) billing. Built with **Java Swing**, **JDBC**, and **MySQL**.

---

## Features

- **Authentication & Role-Based Access Control**: Secure login for Admin and Pharmacist roles.
- **Inventory Management**: Real-time stock tracking, reorder-level alerts, batch logging, and expiry date monitoring.
- **Quick POS / Sales Desk**: Fast stock deductions with atomic inventory validation.
- **Relational Data Integrity**: Foreign key constraints and multi-table consistency backed by MySQL InnoDB engine.

---

## Tech Stack & Prerequisites

- **Language**: Java 17+ (or JDK 8+)
- **GUI Toolkit**: Java Swing (AWT / Swing)
- **Database**: MySQL Server 8.0+
- **Connectivity**: MySQL Connector/J (`mysql-connector-j-8.x.x.jar`)
- **IDE/Editor**: VS Code / IntelliJ IDEA / Eclipse

---

## Database Configuration

1. Start your local MySQL server.
2. Open MySQL CLI or Workbench and run `schema.sql`:
   ```sql
   source /path/to/schema.sql;