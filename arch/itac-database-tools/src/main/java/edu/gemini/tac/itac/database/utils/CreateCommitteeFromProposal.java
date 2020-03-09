package edu.gemini.tac.itac.database.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.gemini.phase1.geminiExt.model.P1Submissions.PartnerCountry;
import edu.gemini.phase1.io.DocBuilder;
import edu.gemini.phase1.io.IDocReader;
import edu.gemini.phase1.model.P1Document;
import edu.gemini.phase1.model.P1Exception;
import edu.gemini.phase1.model.P1ExtensionFactory;
import edu.gemini.tac.persistence.Committee;
import edu.gemini.tac.persistence.LogEntry;
import edu.gemini.tac.persistence.Partner;
import edu.gemini.tac.persistence.Proposal;
import edu.gemini.tac.persistence.phase1.Document;
import edu.gemini.tac.persistence.phase1.GeminiPart;
import edu.gemini.tac.persistence.phase1.PartnerSubmission;
import edu.gemini.tac.persistence.phase1.SubDetailsExtension;
import edu.gemini.tac.persistence.phase1.Submissions;
import edu.gemini.tac.service.CommitteeHibernateService;
import edu.gemini.tac.service.PartnerHibernateService;

public class CreateCommitteeFromProposal {
	private static final List<File> xmlFiles = new ArrayList<File>();
	private static final List<String> unknown = new ArrayList<String>();
	private final String inputDirectoryPath;
	private final String committeeName;
	private ClassPathXmlApplicationContext applicationContext;
	private SessionFactory sessionFactory;
	private SessionFactory p1SessionFactory;
	private CommitteeHibernateService committeeService;
	private PartnerHibernateService partnerService;
	private int problems;
	private List<String> problemDocuments = new ArrayList<String>();
	
	SessionFactory buildItacSessionFactory(final String targetDatabase) {
	    final AnnotationConfiguration configuration = new AnnotationConfiguration();
	    
	    configuration.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
	    configuration.setProperty("hibernate.connection.url","jdbc:postgresql:" + targetDatabase);
	    configuration.setProperty("hibernate.connection.username", "itac");
	    
	    configuration.addAnnotatedClass(edu.gemini.phase1.geminiExt.model.P1Submissions.PartnerCountry.class);
	    configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.coordinatesystem.C1C2System.class);
	    configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.coordinatesystem.ConicSystem.class);
	    configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.coordinatesystem.CoordinateSystem.class);
	    configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.coordinatesystem.DegDegSystem.class);
	    configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.coordinatesystem.HmsDegSystem.class);
	    configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.coordinatesystem.NamedSystem.class);
	    configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.coordinatesystem.NonSiderealSystem.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.Committee.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.LogEntry.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.LogNote.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.Membership.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.Partner.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.Person.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.Proposal.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.Semester.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.Site.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.bandrestriction.BandRestrictionRule.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.bandrestriction.Iq20RestrictionNotInBand3.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.bandrestriction.LgsRestrictionInBandsOneAndTwo.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.bandrestriction.RapidTooRestrictionInBandOne.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.bin.BinConfiguration.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.bin.DecBinSize.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.bin.RABinSize.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.condition.Condition.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.condition.ConditionSet.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.Allocation.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.Attachment.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.AttachmentSet.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.CoInvestigator.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.Common.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.ConstraintSet.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.Contact.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.Document.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.DocumentSite.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.Extension.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.GeminiAllocationExtension.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.GeminiBand3Extension.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.GeminiPart.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.ITacExtension.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.Investigator.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.Investigators.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.KeywordSet.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.Observation.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.ObservationList.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.Observatory.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.PartnerSubmission.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.PrimaryInvestigator.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.ProposalKey.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.ProposalSupport.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.ReferenceList.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.Requirements.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.Resource.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.ResourceReference.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.ResourceCategory.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.ResourceComponent.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.ResourceList.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.ResourceSet.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.Scheduling.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.SiteQuality.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.SiteQualityReference.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.SubDetailsExtension.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.Submissions.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.TacExtension.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.Target.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.TargetReference.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.TargetCatalog.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.TargetSet.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.phase1.Time.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.queues.Queue.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.queues.QueueNote.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.restrictedbin.LgsObservationsRestrictedBin.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.restrictedbin.RestrictedBin.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.restrictedbin.WaterVaporPercentageRestrictedBin.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.security.AuthorityRole.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.queues.Banding.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.queues.ScienceBand.class);
        configuration.addAnnotatedClass(edu.gemini.tac.persistence.queues.ExchangePartnerCharge.class);

        configuration.setProperty("hibernate.dialect","org.hibernate.dialect.PostgreSQLDialect");
        configuration.setProperty("hibernate.cache.region.factory_class","net.sf.ehcache.hibernate.SingletonEhCacheRegionFactory");
        configuration.setProperty("hibernate.cache.use_second_level_cache","true");
        configuration.setProperty("hibernate.cache.use_query_cache","true");
        configuration.setProperty("hibernate.generate_statistics","true");
        configuration.setProperty("hibernate.cache.use_structured_entries","true");

        return configuration.buildSessionFactory();
	}
	
	SessionFactory buildP1SessionFactory(final String targetDatabase) {
	    final AnnotationConfiguration configuration = new AnnotationConfiguration();
	    
	    configuration.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
	    configuration.setProperty("hibernate.connection.url","jdbc:postgresql:" + targetDatabase);
	    configuration.setProperty("hibernate.connection.username", "itac");
	    
        configuration.addAnnotatedClass(edu.gemini.phase1.geminiExt.model.GeminiAllocationExtension.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.geminiExt.model.GeminiBand3Extension.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.geminiExt.model.GeminiPart.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.geminiExt.model.ITacExtension.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.geminiExt.model.P1PartnerSubmission.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.geminiExt.model.P1Submissions.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.geminiExt.model.SiteQuality.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.geminiExt.model.SubDetailsExtension.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.geminiExt.model.TacExtension.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.geminiExt.model.P1Submissions.PartnerCountry.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1Allocation.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1Attachment.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1AttachmentSet.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1C1C2System.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1CoInvestigator.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1Common.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1ConicSystem.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1Contact.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1CoordinateSystem.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1DegDegSystem.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1Document.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1HmsDegSystem.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1Investigator.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1Investigators.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1KeywordSet.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1NamedSystem.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1NodeBaseExtension.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1NonSiderealSystem.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1Observation.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1ObservationList.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1Observatory.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1ObservatoryExtension.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1PrimaryInvestigator.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1ProposalKey.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1ProposalSupport.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1Reference.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1ReferenceListSupport.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1ReferentSupport.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1Requirements.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1Resource.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1ResourceCategory.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1ResourceComponent.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1ResourceList.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1Scheduling.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1Site.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1Target.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1TargetCatalog.class);
        configuration.addAnnotatedClass(edu.gemini.phase1.model.P1Time.class);

        configuration.setProperty("hibernate.dialect","org.hibernate.dialect.PostgreSQLDialect");
        configuration.setProperty("hibernate.cache.region.factory_class","net.sf.ehcache.hibernate.SingletonEhCacheRegionFactory");
        configuration.setProperty("hibernate.cache.use_second_level_cache","true");
        configuration.setProperty("hibernate.cache.use_query_cache","true");
        configuration.setProperty("hibernate.generate_statistics","true");
        configuration.setProperty("hibernate.cache.use_structured_entries","true");

        return configuration.buildSessionFactory();
	}
	
	public CreateCommitteeFromProposal(final String inputDirectoryPath, final String committeeName, final String targetDatabase) {
		this.inputDirectoryPath = inputDirectoryPath;
		this.committeeName = committeeName;
		
		sessionFactory = buildItacSessionFactory(targetDatabase);
		p1SessionFactory = buildP1SessionFactory(targetDatabase);
		
		committeeService = new CommitteeHibernateService();
		committeeService.setSessionFactory(sessionFactory);
		partnerService = new PartnerHibernateService();
		partnerService.setSessionFactory(sessionFactory);
	}

	public void importDirectory() throws P1Exception, FileNotFoundException {
		P1ExtensionFactory.createInstance();

		final Queue<File> directories = new LinkedList<File>(); 
		directories.add(new File(inputDirectoryPath));

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

		
		org.hibernate.classic.Session session = sessionFactory.openSession();
		Transaction transaction = session.beginTransaction();

		final Committee committee = new Committee();
		committee.setName(committeeName);
		committee.setLogEntries(new HashSet<LogEntry>());
		session.save(committee);
		
		final List<Partner> allPartners = partnerService.findAllPartners();
		final Map<String, Partner> partnerMap = new HashMap<String, Partner>();
		for (Partner p : allPartners) {
			partnerMap.put(p.getPartnerCountryKey(), p);
		}
		
		for (File f: xmlFiles) {
			final P1Document p1Document = importDocument(f);
			if (p1Document != null) {
				final Proposal proposal = new Proposal();
				Document document = (Document) session.createQuery("from Document d where d.id = :id").setLong("id", p1Document.getEntityId()).uniqueResult();
				
				proposal.setCommittee(committee);
				proposal.setDocument(document);
	
				PartnerCountry partnerCountry = getPartnerCountry(document);
		    	if (partnerCountry != null) {
		    		Partner partner = partnerMap.get(partnerCountry.getKey());
		    		proposal.setPartner(partner);
		    		session.save(proposal);
		    	} else {
		    		System.out.println("Could not identify partner country:" + f.getName());
		    	}
			}	
		}

    	session.save(committee);
		transaction.commit();
		session.close();
		
		System.out.println(problems + " problems encountered.");
		System.out.println(problemDocuments + " files not imported.");
	}

	private PartnerCountry getPartnerCountry(final Document document) {
    	final GeminiPart gp = document.getGeminiPart();
    	final SubDetailsExtension subDetails = gp.getSubDetails();
    	final Submissions submissions = subDetails.getSubmissions();
    	final Map<PartnerCountry, PartnerSubmission> submissionMap = submissions.getSubmissionMap();
		final Set<PartnerCountry> keySet = submissionMap.keySet();
    	
    	for (PartnerCountry pc : keySet) {
    		final PartnerSubmission partnerSubmission = submissionMap.get(pc);
    		if (partnerSubmission.isSubmissionFlag())
    			return pc;
    	}
    	
    	return null;
    }


	private P1Document importDocument(File f) throws FileNotFoundException, P1Exception {
		final IDocReader docReader = DocBuilder.getReader("xml");
		Session session = p1SessionFactory.openSession();
		Transaction tx = session.beginTransaction();
		System.out.println("Importing " + f.getAbsolutePath());
		P1Document p1Document = docReader.readDocument(new FileReader(f));
		try {
			session.save(p1Document);
			tx.commit();
		} catch (ConstraintViolationException cve) {
			System.out.println("ERROR: Unable to import due to constraint violation.");
			cve.printStackTrace();
			problems++;
			problemDocuments.add(f.getAbsolutePath());
			p1Document = null;
			tx.rollback();
		} finally {
			session.close();
		}

		return p1Document;
	}

	public static void main(final String[] args) throws ParseException, P1Exception, FileNotFoundException {
		System.out.println("args:" + args);
		Options options = new Options();
		options.addOption("i", "inputDirectory", true, "Path of directory to scan for phase 1 documents contained in xml files.");
		options.addOption("c", "committee name", true, "Name of the new committee");
		options.addOption("d", "database context", true, "itac_test, itac_dev, itac_qa");

		HelpFormatter formatter = new HelpFormatter();
		if (args.length < 2) {
			formatter.printHelp( "CreateCommitteeFromProposal", options );
			System.exit(-1);
		}

		final CommandLineParser parser = new PosixParser();
		final CommandLine line = parser.parse( options, args );

		final CreateCommitteeFromProposal committeeCreator = 
			new CreateCommitteeFromProposal(line.getOptionValue("i"), 
					line.getOptionValue("c"), 
					line.getOptionValue("d"));
		committeeCreator.importDirectory();
		
		System.out.println("Don't know what to do with " + unknown);
		System.out.println("Imported " + xmlFiles.size() + " documents.");

	}
}
