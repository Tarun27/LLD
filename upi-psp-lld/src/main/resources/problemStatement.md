Background:

Flipkart is aiming to enter the UPI (Unified Payments Interface) market to enhance its financial ecosystem and improve customer convenience. By integrating UPI, Flipkart can streamline payment processes, reduce dependency on third-party payment gateways, and leverage India's growing digital payment landscape. Additionally, this move aligns with its strategy to offer a seamless and integrated shopping experience, fostering customer loyalty and driving higher transaction volumes.

In order the achieve the above, Flipkart intends to integrate with a Payment Service Provider (PSP) layer so that it can achieve secure and efficient processing of UPI transactions, ensuring seamless fund transfers and real-time payment confirmations without excess regulatory overheads.

Objective:

Design and implement Flipkart UPI.

Features:

User onboarding: Users can be onboarded by providing relevant details (name, phone number, active/deactive status).

List of registered banks: There should be a list of registered banks stored. Linking of bank accounts (next point) can be done only for registered banks. A bank should have the fields bank name and server status (up/down).

Linking a Bank Account: A user should be able to link multiple banks to an account. User Bank details include bank name, bank account number, bank balance (can be initialized a value).

Any one of the bank accounts has to be mandatorily linked to a phone number as a primary bank account.

Making a payment: A user should be able to make a payment to another user by providing username, phone number or bank account.

Handling concurrency: The transactions should be able to handle concurrency.

Transaction history: A user should be able to view all the transactions done at a bank account level and at an user level (cumulative of all bank accounts) [both amount paid and received].

A user should be able to search transactions based on other payee/payer users.

Bonus Points:

Search based on dates and transaction status.
Handling pending payments transactions: PSP layer can return a pending status for a transaction. 
However it is guaranteed that they’ll update the status within 120 seconds. Implement a retry mechanism while handling edge cases.
Important Cases:

A given phone number cannot be linked to more than one user account.
A given bank account cannot be linked to more than one user account.
A user account should be allowed to be marked as activated or deactivated.
Making a payment from one account to another should deduct the balance from the first account and add it to another account.
Payment can be made only from an active account.
Payment to an inactive account should throw relevant errors.
Insufficient bank balance should be handled and throw proper exceptions.
If a bank server is down, any of the bank and payment operations should not be allowed.
Other Notes:

All the payment and bank related user transactions should be done by interacting with the PSP interface. [Bank list and status services should be written outside of this layer since it’s only for configuration and testing purposes].
Add 3 seconds of sleep time to payment operations for testing cases.
Have a driver class or test cases to run the cases.
No need to create a REST API.
Demoable code. Functionalities mentioned above.
Functional completeness is a must. Bonus questions are good to have.
Clean Interface design for the module.
Clean internal design and implementation of the library and the application.
Extensibility
Take care of Exception and Corner case handling.
Test cases covering various cases are good to have.
You are free to use any language of your choice, preferably an Object Oriented language.
Use in memory data structures for storing data.
UTs are good to have.