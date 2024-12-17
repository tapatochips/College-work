/*
Daniel Richmond
cs-210-j8194
Project 3, Corner Grocer
*/

#include <iostream>
#include <fstream>
#include <string>
#include <map>
#include <algorithm>

using namespace std;

//had to use asolute path instead of relative path to get it to work
const string filename = "//apporto.com/dfs/SNHU/USERS/danielrichmon1_snhu/Desktop/Corner Grocer/CS210_Project_Three_Input_File.txt";

//prototype funcs
void loadFrequencies(map<string, int>& frequencies, const string& filename);
void saveFrequencies(const map<string, int>& frequencies, const string& filename);
void printFrequencies(const map<string, int>& frequencies);
void printHistogram(const map<string, int>& frequencies);
void findFrequency(const map < string, int>& frequencies);
int displayMenu();



int main() {
	map <string, int> frequencies; //store using map
	loadFrequencies(frequencies, filename); //load
	bool exit = false;

	//while loop with switch for menu
	while (!exit) {
		switch (displayMenu()) {
		case 1:
			findFrequency(frequencies);
			break;
		case 2:
			printFrequencies(frequencies);
			break;
		case 3:
			printHistogram(frequencies);
			break;
		case 4:
			saveFrequencies(frequencies, "frequency.dat");
			exit = true;
			break;
		}
	}
	return 0;
}


//lowercase func to ensure better search
string toLowerCase(const string& str) {
	string lowerStr = str;
	transform(lowerStr.begin(), lowerStr.end(), lowerStr.begin(), ::tolower);
	return lowerStr;
}

//load from file
void loadFrequencies(map<string, int>& frequencies, const string& filename) {
	ifstream inFile(filename);
	if (!inFile) {
		cerr << "Failed to open " << filename << " for reading." << endl;
		return;
	}
	string item;

	while (getline(inFile, item)) {
		string lowerItem = toLowerCase(item);
		frequencies[lowerItem]++;
	}
	inFile.close();
}

//save items
void saveFrequencies(const map<string, int>& frequencies, const string& filename) {
	ofstream outFile(filename, ios::out);
	if (!outFile) {
		cerr << "Failed to open " << filename << " for writing." << endl;
		return;
	}

	for (const auto& pair : frequencies) {
		outFile << pair.first << " " << pair.second << endl;
	}
}

//print freq of all
void printFrequencies(const map<string, int>& frequencies) {
	for (const auto& pair : frequencies) {
		cout << pair.first << " " << pair.second << endl;
	}
}

//print but histogram
void printHistogram(const map<string, int>& frequencies) {
	for (const auto& pair : frequencies) {
		cout << pair.first << " ";
		string stars(pair.second, '*');
		cout << stars << endl;
	}
}


//find frequency
void findFrequency(const map<string, int>& frequencies) {
	string item;
	cout << "What item are you looking for? " << endl;
	cin >> item;
	cin.ignore(numeric_limits<streamsize>::max(), '\n'); //clear input
	item = toLowerCase(item);

	bool found = false;
	for (const auto& pair : frequencies) {
		if (toLowerCase(pair.first) == item) {
			cout << pair.first << " appears " << pair.second << " times.\n";
			found = true;
			break;
		}
	}
	if (!found) {
		cout << "We can't find that item, try again." << endl;
	}
}

//menu
int displayMenu() {
	int choice;
	cout << "\nMenu\n"
		<< "1. Find the amount of a specific item\n"
		<< "2. Print the amount of all the items\n"
		<< "3. Print a histogram of the items\n"
		<< "4. Exit\n"
		<< "Please enter your choice: ";
	cin >> choice;
	return choice;

}