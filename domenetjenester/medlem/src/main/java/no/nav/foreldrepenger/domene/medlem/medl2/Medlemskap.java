package no.nav.foreldrepenger.domene.medlem.medl2;

import java.time.LocalDate;
import java.util.List;

public interface Medlemskap {

    List<Medlemskapsunntak> finnMedlemsunntak(String aktørId, LocalDate fom, LocalDate tom);

}
