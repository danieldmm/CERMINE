/**
 * This file is part of CERMINE project.
 * Copyright (c) 2011-2013 ICM-UW
 *
 * CERMINE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CERMINE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with CERMINE. If not, see <http://www.gnu.org/licenses/>.
 */

package pl.edu.icm.cermine.bibref.sentiment;

import java.util.List;
import pl.edu.icm.cermine.bibref.model.BibEntry;
import pl.edu.icm.cermine.bibref.sentiment.model.CiTOProperty;
import pl.edu.icm.cermine.bibref.sentiment.model.CitationContext;
import pl.edu.icm.cermine.bibref.sentiment.model.CitationSentiment;

/**
 * Citation sentiment analyser.
 *
 * @author Dominika Tkaczyk
 */
public class DefaultCitationSentimentAnalyser implements CitationSentimentAnalyser {
    
    @Override
    public CitationSentiment analyzeSentiment(BibEntry citation, List<CitationContext> context) {
        CitationSentiment sentiment = new CitationSentiment();
        sentiment.setKey(citation.getKey());
        if (!context.isEmpty()) {
            sentiment.addProperty(CiTOProperty.CITES);
        }
        return sentiment;
    }
}