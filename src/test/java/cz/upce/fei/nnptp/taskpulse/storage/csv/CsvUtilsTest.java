package cz.upce.fei.nnptp.taskpulse.storage.csv;

import cz.upce.fei.nnptp.taskpulse.storage.StorageException;

import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CsvUtilsTest {
    @Test
    void rejectsUnclosedQuoteAtEndOfCSVFile() {
        String csv = "name,description\nProject,\"unfinished";
        
        assertThatThrownBy(() -> CsvUtils.parse(new StringReader(csv)))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("unclosed double quote");
    }
    
    @Test
    void acceptsClosedQuoteAtEndOfCSVFile() {
        String csv = "name,description\nProject,\"finished\"";
        
        assertThatCode(() -> CsvUtils.parse(new StringReader(csv)))
                .doesNotThrowAnyException();
    }
}