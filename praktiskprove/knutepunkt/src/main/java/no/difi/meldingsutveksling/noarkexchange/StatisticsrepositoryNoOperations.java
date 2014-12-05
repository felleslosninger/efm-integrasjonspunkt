package no.difi.meldingsutveksling.noarkexchange;

import java.util.Date;

import eu.peppol.statistics.RawStatistics;
import eu.peppol.statistics.RawStatisticsRepository;
import eu.peppol.statistics.RawStatisticsRepositoryFactory;
import eu.peppol.statistics.StatisticsGranularity;
import eu.peppol.statistics.StatisticsTransformer;

public class StatisticsrepositoryNoOperations implements RawStatisticsRepositoryFactory,  RawStatisticsRepository{

	@Override
	public RawStatisticsRepository getInstanceForRawStatistics() {
		return new StatisticsrepositoryNoOperations();
	}

	@Override
	public Integer persist(RawStatistics rawStatistics) {
		return 0;
	}

	@Override
	public void fetchAndTransformRawStatistics(StatisticsTransformer transformer, Date start, Date end, StatisticsGranularity granularity) {		
	}

}
