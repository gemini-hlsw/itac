package edu.gemini.tac.itac.database.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.hibernate.Session;
import org.hibernate.Transaction;

import edu.gemini.phase1.geminiExt.model.persistence.HibernateUtil;
import edu.gemini.phase1.io.DocBuilder;
import edu.gemini.phase1.io.IDocReader;
import edu.gemini.phase1.model.P1Document;
import edu.gemini.phase1.model.P1Exception;
import edu.gemini.phase1.model.P1ExtensionFactory;

/**
 * Class supporting import of proposals contained in xml files into the database using
 * the p1Model and gemini-p1Model libraries.  Currently uses the simplified
 * HibernateUtils approach, may need to be reworked to use data source
 * injection at some point in the future.
 * 
 * Other ideas include producing more permanent reports, and potentially
 * doing an export/re-import to confirm that everything is surviving the process.
 * 
 * @author ddawson
 *
 */
public class ImportProposals {
	final Queue<File> xmlFiles = new LinkedList<File>();
	final Queue<File> directories = new LinkedList<File>();
	final List<String> unknown = new LinkedList<String>();
	final Set<String> proposalKeys = new HashSet<String>();
	final Set<String> problemProposalKeys = new HashSet<String>();

	/**
	 * @param dataDirectory - Source directory to begin scanning for proposals in xml format.
	 * @param batch - Should this be batched into transactions, or a transaction per document
	 * @throws P1Exception
	 * @throws FileNotFoundException
	 */
	public void importProposals(final String dataDirectory, final boolean batch) throws P1Exception, FileNotFoundException {
		final IDocReader docReader = DocBuilder.getReader("xml");
		P1ExtensionFactory.createInstance();

		directories.add(new File(dataDirectory));

		while (!directories.isEmpty()) {
			final File[] allFiles = directories.poll().listFiles();
			for (File f : allFiles) {
				System.out.println("Examining " + f.getAbsolutePath());

				if (f.getName().endsWith("xml"))
					xmlFiles.add(f);
				else if (f.isDirectory())
					directories.add(f);
				else
					unknown.add(f.getName());
			}
		}


		if (batch)
			batchedImport(docReader);
		else 
			importProposals(docReader);

		System.out.println("Don't know what to do with " + unknown);
		System.out.println("Imported " + xmlFiles.size() + " documents.");

	}
	private void batchedImport(final IDocReader docReader)
	throws P1Exception, FileNotFoundException {
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = session.beginTransaction();

		int importCount = 0;
		for (File f : xmlFiles) {
			System.out.println("Importing " + f.getAbsolutePath());
			final P1Document p1Document = docReader.readDocument(new FileReader(f));
			final String proposalKey = p1Document.getProposalKey().getStringRepresentation();
			if (!proposalKeys.contains(proposalKey))
				proposalKeys.add(proposalKey);
			else {
				System.out.println("PROPOSAL KEY DUPLICATE " + proposalKey + " from " + f.getAbsolutePath());
				problemProposalKeys.add(proposalKey);
			}

			session.save(p1Document);
			System.out.println("Proposal key " + p1Document.getProposalKey());
			System.out.println("Imported " + f.getAbsolutePath());
			if ((++importCount % 50) == 0) {
				System.out.println("Committing transaction...");
				tx.commit();
				tx = session.beginTransaction();
			}
		}
		tx.commit();
		session.close();
	}

	private void importProposals(final IDocReader docReader)
		throws P1Exception, FileNotFoundException {

		for (File f : xmlFiles) {
			Session session = HibernateUtil.getSessionFactory().openSession();
			Transaction tx = session.beginTransaction();
			System.out.println("Importing " + f.getAbsolutePath());
			final P1Document p1Document = docReader.readDocument(new FileReader(f));
			final String proposalKey = p1Document.getProposalKey().getStringRepresentation();
			if (!proposalKeys.contains(proposalKey))
				proposalKeys.add(proposalKey);
			else
				System.out.println("PROPOSAL KEY DUPLICATE " + proposalKey + " from " + f.getAbsolutePath());

			session.save(p1Document);
			tx.commit();
			session.close();
			System.out.println("Proposal key " + p1Document.getProposalKey());
			System.out.println("Imported " + f.getAbsolutePath());
		}
	}


	public static void main(String[] args) throws P1Exception, FileNotFoundException, ParseException {
		System.out.println("args:" + args);
		Options options = new Options();
		options.addOption("i", "inputDirectory", true, "Path of directory to scan for phase 1 documents contained in xml files.");
		options.addOption("b", "batch", false, "Boolean with presence indicating that import commits should be batched");
		
		HelpFormatter formatter = new HelpFormatter();
		if (args.length < 2){
			formatter.printHelp( "ImportProposals", options );
			System.exit(-1);
		}

		
		final CommandLineParser parser = new PosixParser();
		final CommandLine line = parser.parse( options, args );
		
		final ImportProposals importProposals = new ImportProposals();
		importProposals.importProposals(line.getOptionValue('i'), line.hasOption('b'));
	}
}
