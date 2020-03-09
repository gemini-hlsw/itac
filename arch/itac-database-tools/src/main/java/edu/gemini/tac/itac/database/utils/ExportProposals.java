package edu.gemini.tac.itac.database.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.stat.SecondLevelCacheStatistics;
import org.hibernate.stat.Statistics;

import com.ibm.icu.util.Calendar;

import edu.gemini.phase1.geminiExt.model.P1PartnerSubmission;
import edu.gemini.phase1.geminiExt.model.P1Submissions;
import edu.gemini.phase1.geminiExt.model.SubDetailsExtension;
import edu.gemini.phase1.geminiExt.model.TacExtension;
import edu.gemini.phase1.geminiExt.model.persistence.HibernateUtil;
import edu.gemini.phase1.io.DocBuilder;
import edu.gemini.phase1.io.IDocWriter;
import edu.gemini.phase1.model.IP1Extension;
import edu.gemini.phase1.model.IP1ObservatoryExtension;
import edu.gemini.phase1.model.P1Document;
import edu.gemini.phase1.model.P1Exception;
import edu.gemini.phase1.model.P1ExtensionFactory;
import edu.gemini.phase1.model.P1Observatory;
import edu.gemini.phase1.model.P1Target;
import edu.gemini.phase1.model.P1TargetCatalog;

public class ExportProposals {
	private static final int PAGE_SIZE = 1;
	int proposalsExported = 0;
	long beginTime;

	public ExportProposals() {
		beginTime = System.currentTimeMillis();
	}
	
	public class StatDumper extends TimerTask {
		final private String region;

		public StatDumper(final String region) {
			this.region = region;
		}

		public void run() {
			SecondLevelCacheStatistics secondLevelCacheStatistics = HibernateUtil.getSessionFactory().getStatistics().getSecondLevelCacheStatistics(region);
			if (secondLevelCacheStatistics != null) {
				System.out.println(region + " SInMemory:" + secondLevelCacheStatistics.getElementCountInMemory());
				System.out.println(region + " SHitCount:" + secondLevelCacheStatistics.getHitCount());
				System.out.println(region + " SMissCount:" + secondLevelCacheStatistics.getMissCount());
				System.out.println(region + " SPutCount:" + secondLevelCacheStatistics.getPutCount());
			} else {
				System.out.println("Unable to create 2nd level stats for " + region);
			}
		}
	}

	public static void main(String[] args) throws FileNotFoundException, P1Exception, ParseException {
		Options options = new Options();
		options.addOption("o", "outputDirectory", true, "Path of directory to serve as root for exporting documents.");
		options.addOption("c", "command", true, "[all, {id of document to be exported}");

		HelpFormatter formatter = new HelpFormatter();
		if (args.length < 2){
			formatter.printHelp( "ExportProposals", options );
			System.exit(-1);
		}

		final CommandLineParser parser = new PosixParser();
		final CommandLine line = parser.parse( options, args );
		final ExportProposals exportProposals = new ExportProposals();
		final List<Timer> statTimers = exportProposals.createAndScheduleTimers();
		
		if (line.getOptionValue('c').equals("all"))
			exportProposals.exportAllProposals(line.getOptionValue('o'));
		else 
			exportProposals.exportOneProposal(Long.parseLong(line.getOptionValue('c')), line.getOptionValue('o'));

		for (Timer t : statTimers)
			t.cancel();

	}

	private void exportOneProposal(final Long id, final String outputDirectory) throws P1Exception, FileNotFoundException {
		P1ExtensionFactory.createInstance();

		System.out.println("Exporting proposal" + id);
		long initialTime = System.currentTimeMillis();
		Session session = HibernateUtil.getSessionFactory().openSession();
		session.setDefaultReadOnly( true );
		Transaction transaction = session.beginTransaction();
		final P1Document d = fetchDocument(id, session);
		if (d == null) {
			System.out.println("No document with that id identified, please doublecheck.");
			System.exit(-1);
		}
		

		System.out.println(("From database " + (System.currentTimeMillis() - initialTime) / 1000d) + " seconds elapsed");
		final IDocWriter xmlWriter = DocBuilder.getWriter("xml");
		System.out.println("Processing proposal with key " + d.getProposalKey().getStringRepresentation());

		final List<P1Observatory> observatories = d.getObservatories();
		final P1Observatory gemini = observatories.get(0);
		final IP1ObservatoryExtension observatoryExt = gemini.getObservatoryExt();
		final List<IP1Extension> extensions = observatoryExt.getExtensions();
		SubDetailsExtension subDetailsExtension = null;
		TacExtension tacExtension = null;
		for (final IP1Extension e : extensions) {
			if (e.getType().equals("subDetails"))
				subDetailsExtension = (SubDetailsExtension) e;
			else if (e.getType().equals("tac"))
				tacExtension = (TacExtension) e;
		}

		final P1Submissions submissions = subDetailsExtension.getSubmissions();
		final List<P1PartnerSubmission> allSubmissions = submissions.getAll();

		P1PartnerSubmission hostSubmission = allSubmissions.get(0);
		for (final P1PartnerSubmission submission : allSubmissions) {
			if (submission.getPartner().equals(subDetailsExtension.getHostPartner()))
				hostSubmission = submission;
		}


		int year = getYear(subDetailsExtension, tacExtension);
		final String partnerKey = hostSubmission.getPartner().getKey();

		final File yearDirectory = new File(outputDirectory + "/" + year);
		if (!yearDirectory.exists())
			yearDirectory.mkdir();
		final File partnerDirectory = new File(outputDirectory + "/" + year + "/" + partnerKey);
		if (!partnerDirectory.exists())
			partnerDirectory.mkdir();

		final File file = new File(outputDirectory + "/" + year + "/" + partnerKey + "/" + hostSubmission.getPartnerReferenceNumber().replaceAll("/","-") + ".xml");

		long writeStartTime = System.currentTimeMillis();
		final FileOutputStream writer = new FileOutputStream(file);
		System.out.println("Writing...");
		xmlWriter.writeDocument(d, d.getObservatories(), writer);
		System.out.println(("Write time " + (System.currentTimeMillis() - writeStartTime) / 1000d) + " seconds elapsed");
		System.out.println(("Overall " + (System.currentTimeMillis() - initialTime) / 1000d) + " seconds elapsed");
		transaction.commit();
		session.close();
		
	}

	private List<Timer> createAndScheduleTimers() {
		final int interval = 60 * 1000;
		final int longInterval = interval * 5;
		final List<Timer> timers = new ArrayList<Timer>();
		final String[] regions = {"edu.gemini.phase1.model.P1Target","edu.gemini.phase1.model.P1Resource",
				"edu.gemini.phase1.model.P1Reference","edu.gemini.phase1.model.P1ReferentSupport"
		};
		for (final String r : regions) {
			final Timer t = new Timer();
			t.schedule(new StatDumper(r), new Date(), longInterval);
			timers.add(t);
			
		}
		final Timer t = new Timer();
		t.schedule(new TimerTask() {
			public void run() {
				final Statistics stats = HibernateUtil.getSessionFactory().getStatistics();

				final double queryCacheHitCount  = stats.getQueryCacheHitCount();
				final double queryCacheMissCount = stats.getQueryCacheMissCount();
				final double queryCacheHitRatio =
					queryCacheHitCount / (queryCacheHitCount + queryCacheMissCount);

				System.out.println("queryCacheHitCount  :" + queryCacheHitCount);
				System.out.println("queryCacheMissCount  :" + queryCacheMissCount);
				System.out.println("Query Hit ratio:" + queryCacheHitRatio);
			}
		}, new Date(), interval);
		timers.add(t);
		
		final Timer t2 = new Timer();
		t.schedule(new TimerTask() {
			public void run() {
				double elapsed = (System.currentTimeMillis() - beginTime) / 1000.0d;
				System.out.println("elapsed time:" + elapsed / 60);
				System.out.println("exports/minute:" + proposalsExported / (elapsed * 60.0d));
				
			}
		}, new Date(), interval);
		timers.add(t2);
		
		return timers;
	}

	private P1Document fetchDocument(final Long id, Session session) {
		System.out.println("Starting fetch");
		final P1Document d = (P1Document) session.createQuery("from P1Document d " + 
				"left join fetch d._common as c " +
				"left join fetch c._keywordSet as ks " +
				"left join fetch c._scientificJustification as sj " + 
				"left join fetch c._targetCatalog as tc " +
				//				"left join fetch sj._attachments as attachs" + // Multiple bag problem 
				//				"left join fetch tc._targets as targets " + // Multiple bag problem
				"left join fetch d._observatories as obs " +
				"left join fetch d._proposalKey as dpk " + 
		"where d.id = :id").setLong("id", id).setReadOnly(true).uniqueResult();

		d.getCommon().getScientificJustification().getAttachments();
		//		P1TargetCatalog targetCatalog = d.getCommon().getTargetCatalog();
		session.createQuery("from P1Common c left join fetch c._targetCatalog tc left join fetch tc._targets t left join fetch t._coordinates c left join fetch t._refSup rs where c.id = (:id)").setLong("id",d.getCommon().getEntityId()).list();
		//		List<Long> targetIds = new ArrayList<Long>();
		//		for (P1Target t : targets) {
		//			targetIds.add(t.getEntityId());
		//		}
		//		session.createQuery("from P1ReferentSupport p left join fetch p._refList rl where p.id in (:ids)").setParameterList("ids", targetIds).list();
		//		P1ObservationList observationList = d.getObservatories().get(0).getObservationList();
		//		session.createQuery("from P1Observation op left join fetch op._targetListSup tls left join fetch tls._refList rl where op.id = :id").setLong("id", observationList.getEntityId()).list();
		//		d.getObservatories().get(0).getProposalSupport().getAllocations();
		//		d.getObservatories().get(0).getSelectedResources().getResourceCategories();
		//		d.getObservatories().get(0).getTechnicalJustification().getAttachments();
		//		((GeminiPart) d.getObservatories().get(0).getObservatoryExt()).getTacExtensions();
		//		System.out.println("Fetch ended");

//		P1TargetCatalog targetCatalog = d.getCommon().getTargetCatalog();
//		List<P1Target> targets = targetCatalog.getTargets();
//		for (P1Target t : targets) {
//			t.setParent(targetCatalog);
//		}

		
		return d;
	}

	private int getYear(SubDetailsExtension subDetailsExtension,
			TacExtension tacExtension) {
		Date dateForDirectoryCreation = null;
		final Date geminiReceivedDate = subDetailsExtension.getGeminiReceivedDate();
		final Date partnerReceivedDate = tacExtension.getPartnerReceivedDate();
		if (geminiReceivedDate == null)
			dateForDirectoryCreation = partnerReceivedDate;
		else
			dateForDirectoryCreation = geminiReceivedDate;

		int year = 0;
		if (dateForDirectoryCreation != null) {
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(dateForDirectoryCreation);
			year = calendar.get(Calendar.YEAR);
		}
		return year;
	}


	@SuppressWarnings("unchecked")
	private void exportAllProposals(final String baseDirectory) throws P1Exception, FileNotFoundException {
		P1ExtensionFactory.createInstance();

		System.out.println("Exporting proposals");
		Session session = HibernateUtil.getSessionFactory().openSession();
		final Long documentCount = ((Long) session.createQuery("select count(*) from P1Document d").iterate().next() ).longValue();
		System.out.println(documentCount + " documents to be exported.");
		session.close();
		int pageNumber = 0;

		while ((pageNumber + 1) * PAGE_SIZE < documentCount) {
			System.out.println("Retrieving document " + pageNumber * PAGE_SIZE);
			session = HibernateUtil.getSessionFactory().openSession();
			final Query query = session.createQuery("from P1Document d");
			System.out.println("Beginning retrieval of proposals from database");
			query.setMaxResults(PAGE_SIZE);
			query.setFirstResult(PAGE_SIZE * pageNumber );
			final List<P1Document> list = query.list();
			for (final P1Document d : list) {
				long initialTimeMillis = System.currentTimeMillis();
				final IDocWriter xmlWriter = DocBuilder.getWriter("xml");
				System.out.println("Processing proposal with key " + d.getProposalKey().getStringRepresentation());

				final List<P1Observatory> observatories = d.getObservatories();
				final P1Observatory gemini = observatories.get(0);
				final IP1ObservatoryExtension observatoryExt = gemini.getObservatoryExt();
				final List<IP1Extension> extensions = observatoryExt.getExtensions();
				SubDetailsExtension subDetailsExtension = null;
				TacExtension tacExtension = null;
				for (final IP1Extension e : extensions) {
					if (e.getType().equals("subDetails"))
						subDetailsExtension = (SubDetailsExtension) e;
					else if (e.getType().equals("tac"))
						tacExtension = (TacExtension) e;
				}

				final P1Submissions submissions = subDetailsExtension.getSubmissions();
				final List<P1PartnerSubmission> allSubmissions = submissions.getAll();

				P1PartnerSubmission hostSubmission = allSubmissions.get(0);
				for (final P1PartnerSubmission submission : allSubmissions) {
					if (submission.getPartner().equals(subDetailsExtension.getHostPartner()))
						hostSubmission = submission;

				}

				int year = getYear(subDetailsExtension, tacExtension);
				final String partnerKey = hostSubmission.getPartner().getKey();

				final File yearDirectory = new File(baseDirectory + "/" + year);
				if (!yearDirectory.exists())
					yearDirectory.mkdir();
				final File partnerDirectory = new File(baseDirectory + "/"  + year + "/" + partnerKey);
				if (!partnerDirectory.exists())
					partnerDirectory.mkdir();

				final File file = new File(baseDirectory + "/"  + year + "/" + partnerKey + "/" + hostSubmission.getPartnerReferenceNumber().replaceAll("/","-") + ".xml");

				final FileOutputStream writer = new FileOutputStream(file);
				System.out.println("Writing...");
				xmlWriter.writeDocument(d, d.getObservatories(), writer);
				long elapsed = System.currentTimeMillis() - initialTimeMillis;
				System.out.println(("Overall " + elapsed / 1000d) + " seconds elapsed");
				proposalsExported++;
			}

			session.close();
			pageNumber++;
		}
	}
}
