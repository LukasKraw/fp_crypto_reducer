╔═══════════════════╗
║ FP-CRYPTO-REDUCER ║
╚═══════════════════╝

This program is a working prototype of an approach to reduce certain types of false positives in vulnerability reports of SAST tools, that target code using cryptographic libraries.
The following steps are necessary to make the prototype work:

1. Import the files as a project into Eclipse
2. Have the program you want to analyse ready as another project in Eclipse with the binaries generated
3. Install the CogniCrypt Plugin for Eclipse
4. Analyze the program from 2. with CogniCrypt and copy the output from the Eclipse console into a .txt-file (see the example .txt-file provided)
5. Delete any line in the .txt-file that does not represent a vulnerability found by CogniCrypt
6. Create another .txt-file containing the Sink definitions (see the example .txt-file provided)
7. When running the prototype, provide the following command line arguments in that order:
		"Absolute Path to src directory of program you want to analyse"
		"Absolute Path to bin directory of program you want to analyse"
		"Absolute Path to .txt-file containing the CogniCrypt report"
		"Absolute Path to .txt-file containing the Sink definitions"
