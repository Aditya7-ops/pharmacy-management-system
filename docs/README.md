# Pharmacy Management System (Java Swing + MySQL)

## Current project features
- Add, update, delete, and search medicines.
- Display medicine inventory in a JTable.
- Store medicine name, category, manufacturer, expiry date, stock quantity, and price.
- Sell a selected medicine, check stock, reduce stock, and record the sale in MySQL.
- Uses JDBC prepared statements for database operations.

## Requirements
- JDK 8 or newer
- Eclipse IDE
- MySQL Server / MySQL Workbench
- MySQL Connector/J JAR

## Setup
1. Open MySQL Workbench and run `database/pharmacy_db.sql`.
2. In `src/pharmacy/DBConnection.java`, replace `YOUR_MYSQL_PASSWORD` with your MySQL root password.
3. In Eclipse, create a Java project named `PharmacyManagementSystem`.
4. Copy the `src/pharmacy` folder into the project's `src` folder.
5. Add MySQL Connector/J to the project's build path:
   Project > Properties > Java Build Path > Libraries > Classpath > Add External JARs.
6. Run `pharmacy.PharmacyManagement`.

## Date format
Enter expiry dates as YYYY-MM-DD, for example `2027-12-31`.

## Important
- This is a student project starter, not a production pharmacy or clinical system.
- Sample medicines are dummy data for testing only.
- Final report should be prepared after testing the application and capturing actual screenshots.
