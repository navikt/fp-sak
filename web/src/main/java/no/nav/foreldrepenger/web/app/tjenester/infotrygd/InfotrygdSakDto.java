package no.nav.foreldrepenger.web.app.tjenester.infotrygd;

import java.time.LocalDate;

public record InfotrygdSakDto(LocalDate iverksatt, String resultat, LocalDate registrert, String sakId, LocalDate mottatt,
                              String type, LocalDate vedtatt, String valg, String undervalg, String nivaa) {

}
