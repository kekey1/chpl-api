package gov.healthit.chpl.scheduler.job.summarystatistics.pdf;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.itextpdf.layout.element.Table;

import gov.healthit.chpl.dao.CertificationBodyDAO;
import gov.healthit.chpl.scheduler.job.summarystatistics.data.EmailStatistics;


@Component
public class DeveloperSummaryStatisticsSectionPdf extends SummaryStatisticsSectionPdf {

    @Autowired
    public DeveloperSummaryStatisticsSectionPdf(CertificationBodyDAO certificationBodyDAO) {
        super(certificationBodyDAO);
    }

    @Override
    public Table generateTable(LocalDate recent, LocalDate previous, EmailStatistics recentEmailStatistics, EmailStatistics previousEmailStatistics) {
        Table table = new Table(getRelativeColumnWidths());
        table.useAllAvailableWidth();
        table = addHeaders(table, "Developer Statistics", recent, previous);
        table = addTableRow(table, createDataForRow("1. Total # of Unique Developers (Regardless of Edition)",
                                    recentEmailStatistics.getDevelopersForEditionAllAndAllStatuses(),
                                    previousEmailStatistics.getDevelopersForEditionAllAndAllStatuses()), true);

        table = addTableRow(table, createDataForRow("a. Total # of Developers with 2014 Listings*",
                recentEmailStatistics.getDevelopersForEdition2014WithAllStatuses().getCount(),
                previousEmailStatistics.getDevelopersForEdition2014WithAllStatuses().getCount()),
                NUMBER_OF_INDENTS_SUMMARY_LEVEL_STAT, true);

        table = addAcbRows(table,
                recentEmailStatistics.getDevelopersForEdition2014WithAllStatuses().getAcbStatistics(),
                previousEmailStatistics.getDevelopersForEdition2014WithAllStatuses().getAcbStatistics());

        table = addTableRow(table, createDataForRow("b. Total # of Developers with Active 2014 Listings*",
                recentEmailStatistics.getDevelopersForEdition2014WithActiveStatuses().getCount(),
                previousEmailStatistics.getDevelopersForEdition2014WithActiveStatuses().getCount()),
                NUMBER_OF_INDENTS_SUMMARY_LEVEL_STAT, true);

        table = addAcbRows(table,
                recentEmailStatistics.getDevelopersForEdition2014WithActiveStatuses().getAcbStatistics(),
                previousEmailStatistics.getDevelopersForEdition2014WithActiveStatuses().getAcbStatistics());

        table = addTableRow(table, createDataForRow("c. Total # of Developers with Suspended by ONC-ACB/ONC 2014 Listings*",
                recentEmailStatistics.getDevelopersForEdition2014WithSuspendedStatuses().getCount(),
                previousEmailStatistics.getDevelopersForEdition2014WithSuspendedStatuses().getCount()),
                NUMBER_OF_INDENTS_SUMMARY_LEVEL_STAT, true);

        table = addAcbRows(table,
                recentEmailStatistics.getDevelopersForEdition2014WithSuspendedStatuses().getAcbStatistics(),
                previousEmailStatistics.getDevelopersForEdition2014WithSuspendedStatuses().getAcbStatistics());


        table = addTableRow(table, createDataForRow("d. Total # of Developers with 2015 Listings or 2015 Cures Update Listings (Regardless of Status)*",
                recentEmailStatistics.getDevelopersForEdition2015CuresAndNonCuresWithAllStatuses().getCount(),
                previousEmailStatistics.getDevelopersForEdition2015CuresAndNonCuresWithAllStatuses().getCount()),
                NUMBER_OF_INDENTS_SUMMARY_LEVEL_STAT, true);

        table = addAcbRows(table,
                recentEmailStatistics.getDevelopersForEdition2015CuresAndNonCuresWithAllStatuses().getAcbStatistics(),
                previousEmailStatistics.getDevelopersForEdition2015CuresAndNonCuresWithAllStatuses().getAcbStatistics());


        table = addTableRow(table, createDataForRow("e. Total # of Developers with Active 2015 Listings or 2015 Cures Update Listings*",
                recentEmailStatistics.getDevelopersForEdition2015CuresAndNonCuresWithActiveStatuses().getCount(),
                previousEmailStatistics.getDevelopersForEdition2015CuresAndNonCuresWithActiveStatuses().getCount()),
                NUMBER_OF_INDENTS_SUMMARY_LEVEL_STAT, true);

        table = addAcbRows(table,
                recentEmailStatistics.getDevelopersForEdition2015CuresAndNonCuresWithActiveStatuses().getAcbStatistics(),
                previousEmailStatistics.getDevelopersForEdition2015CuresAndNonCuresWithActiveStatuses().getAcbStatistics());

        table = addTableRow(table, createDataForRow("f.  Total # of Developers with Suspended by ONC-ACB/Suspended by ONC 2015 Listings or 2015 Cures Update Listings*",
                recentEmailStatistics.getDevelopersForEdition2015CuresAndNonCuresWithSuspendedStatuses().getCount(),
                previousEmailStatistics.getDevelopersForEdition2015CuresAndNonCuresWithSuspendedStatuses().getCount()),
                NUMBER_OF_INDENTS_SUMMARY_LEVEL_STAT, true);

        table = addAcbRows(table,
                recentEmailStatistics.getDevelopersForEdition2015CuresAndNonCuresWithSuspendedStatuses().getAcbStatistics(),
                previousEmailStatistics.getDevelopersForEdition2015CuresAndNonCuresWithSuspendedStatuses().getAcbStatistics());

        table = addTableRow(table, createDataForRow("g. Total # of Developers with 2015 Listings (Regardless of Status)",
                recentEmailStatistics.getDevelopersForEdition2015NonCuresWithAllStatuses().getCount(),
                previousEmailStatistics.getDevelopersForEdition2015NonCuresWithAllStatuses().getCount()),
                NUMBER_OF_INDENTS_SUMMARY_LEVEL_STAT, true);

        table = addAcbRows(table,
                recentEmailStatistics.getDevelopersForEdition2015NonCuresWithAllStatuses().getAcbStatistics(),
                previousEmailStatistics.getDevelopersForEdition2015NonCuresWithAllStatuses().getAcbStatistics());

        table = addTableRow(table, createDataForRow("h. Total # of Developers with Active 2015 Listings",
                recentEmailStatistics.getDevelopersForEdition2015NonCuresWithActiveStatuses().getCount(),
                previousEmailStatistics.getDevelopersForEdition2015NonCuresWithActiveStatuses().getCount()),
                NUMBER_OF_INDENTS_SUMMARY_LEVEL_STAT, true);

        table = addAcbRows(table,
                recentEmailStatistics.getDevelopersForEdition2015NonCuresWithActiveStatuses().getAcbStatistics(),
                previousEmailStatistics.getDevelopersForEdition2015NonCuresWithActiveStatuses().getAcbStatistics());

        table = addTableRow(table, createDataForRow("i. Total # of Developers with Suspended by ONC-ACB/Suspended by ONC 2015 Listings",
                recentEmailStatistics.getDevelopersForEdition2015NonCuresWithSuspendedStatuses().getCount(),
                previousEmailStatistics.getDevelopersForEdition2015NonCuresWithSuspendedStatuses().getCount()),
                NUMBER_OF_INDENTS_SUMMARY_LEVEL_STAT, true);

        table = addAcbRows(table,
                recentEmailStatistics.getDevelopersForEdition2015NonCuresWithSuspendedStatuses().getAcbStatistics(),
                previousEmailStatistics.getDevelopersForEdition2015NonCuresWithSuspendedStatuses().getAcbStatistics());

        table = addTableRow(table, createDataForRow("j. Total # of Developers with 2015 Cures Update Listings (Regardless of Status)",
                recentEmailStatistics.getDevelopersForEdition2015CuresWithAllStatuses().getCount(),
                previousEmailStatistics.getDevelopersForEdition2015CuresWithAllStatuses().getCount()),
                NUMBER_OF_INDENTS_SUMMARY_LEVEL_STAT, true);

        table = addAcbRows(table,
                recentEmailStatistics.getDevelopersForEdition2015CuresWithAllStatuses().getAcbStatistics(),
                previousEmailStatistics.getDevelopersForEdition2015CuresWithAllStatuses().getAcbStatistics());

        table = addTableRow(table, createDataForRow("k. Total # of Developers with Active 2015 Cures Update Listings",
                recentEmailStatistics.getDevelopersForEdition2015CuresWithActiveStatuses().getCount(),
                previousEmailStatistics.getDevelopersForEdition2015CuresWithActiveStatuses().getCount()),
                NUMBER_OF_INDENTS_SUMMARY_LEVEL_STAT, true);

        table = addAcbRows(table,
                recentEmailStatistics.getDevelopersForEdition2015CuresWithActiveStatuses().getAcbStatistics(),
                previousEmailStatistics.getDevelopersForEdition2015CuresWithActiveStatuses().getAcbStatistics());

        table = addTableRow(table, createDataForRow("l. Total # of Developers with Suspended by ONC-ACB/Suspended by ONC 2015 Cures Update Listings",
                recentEmailStatistics.getDevelopersForEdition2015CuresWithSuspendedStatuses().getCount(),
                previousEmailStatistics.getDevelopersForEdition2015CuresWithSuspendedStatuses().getCount()),
                NUMBER_OF_INDENTS_SUMMARY_LEVEL_STAT, true);

        table = addAcbRows(table,
                recentEmailStatistics.getDevelopersForEdition2015CuresWithSuspendedStatuses().getAcbStatistics(),
                previousEmailStatistics.getDevelopersForEdition2015CuresWithSuspendedStatuses().getAcbStatistics());

        return table;
    }
}
