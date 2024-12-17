/*
Daniel Richmond
cs210-j8184
2/2/2024
Airgead Banking app
*/

#include <iostream>
#include <iomanip>

using namespace std;

//display menu
void displayMenu() {
	cout << "********************************\n";
	cout << "********** Data Input **********\n";
	cout << "Initial Investment Amount: \n";
	cout << "Monthly Deposit: \n";
	cout << "Annual Interest: \n";
	cout << "Number of Years: \n";
	cout << "Press any key to continue. . . .\n";
}

//calc and show growth without
void displayWithoutMonthly(double initialInvestment, double annualInterest, int years) {
	double openingAmount = initialInvestment;
	double interest;
	double closingBalance;

	cout << "\nBalance and Interest Without Additional Monthly Deposits\n";
	cout << "========================================================\n";
	cout << "Year\tYear End Balance\tYear End Earneed Interest\n";
	cout << "--------------------------------------------------------\n";

	for (int i = 1; i <= years; i++) {
		interest = openingAmount * (annualInterest / 100);
		closingBalance = openingAmount + interest;
		cout << i << "\t$" << fixed << setprecision(2) << closingBalance << "\t\t\t$" << interest << "\n";
		openingAmount = closingBalance;
	}
}

//calc and show growth with
void displayWithMonthly(double initialInvestment, double monthlyDeposit, double annualInterest, int years) {
	double openingAmount = initialInvestment;
	double interest;
	double closingBalance;

	cout << "\nBalance and Interest With Additional Monthly Deposits\n";
	cout << "======================================================\n";
	cout << "Year\tYear End Balance\tYear End Earned Interest\n";
	cout << "------------------------------------------------------\n";

	for (int i = 1; i <= years; i++) {
		double yearEndInterest = 0;
		for (int j = 0; j < 12; j++) {
			interest = (openingAmount + monthlyDeposit) * (annualInterest / 100 / 12);
			yearEndInterest += interest;
			openingAmount += monthlyDeposit + interest;
		}
		closingBalance = openingAmount;
		cout << i << "\t$" << fixed << setprecision(2) << closingBalance << "\t\t\t$" << yearEndInterest << "\n";
	}
}

int main() {
	double initialInvestment, monthlyDeposit, annualInterest;
	int years;

	displayMenu();
	//actual code to get live data, comment out testing down below and un-comment this to run it this way
	// cout << "Enter initial investment amount: ";
	// cin >> initialInvestment;
	// cout << "Enter monthly deposit: ";
	// cin >> monthlyDeposit;
	// cout << "Enter annual interest: ";
	// cin >> annualInterest;
	// cout << "Enter number of years: ";
	// cin >> years;

	//for testing
	initialInvestment = 1000; // test
	monthlyDeposit = 100; // test
	annualInterest = 5; // test
	years = 10; // test

	//calc, show, without
	displayWithoutMonthly(initialInvestment, annualInterest, years);

	//calc, show, with
	displayWithMonthly(initialInvestment, monthlyDeposit, annualInterest, years);

	return 0;
}