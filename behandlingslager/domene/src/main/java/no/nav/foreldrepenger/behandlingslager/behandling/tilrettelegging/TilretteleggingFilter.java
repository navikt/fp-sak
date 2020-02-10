package no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Brukt til å filtrere tilrettelegginger for SVP.
 *
 * Benytter seg av de overstyrte tilretteleggingene hvor mulig.
 *
 */
public class TilretteleggingFilter {

    private SvpTilretteleggingerEntitet opprinneligeTilrettelegginger;
    private SvpTilretteleggingerEntitet overstyrteTilrettelegginger;

    public TilretteleggingFilter(SvpGrunnlagEntitet svpGrunnlagEntitet) {
        this.opprinneligeTilrettelegginger = svpGrunnlagEntitet.getOpprinneligeTilrettelegginger();
        this.overstyrteTilrettelegginger = svpGrunnlagEntitet.getOverstyrteTilrettelegginger();
    }

    /**
     * Tar _IKKE_ hensyn til hvorvidt tilretteleggingen skal brukes eller ikke.
     *
     * Bruk {@link #getAktuelleTilretteleggingerFiltrert} hvis du kun skal benytte de tilretteleggingene
     * som saksbehandler har valgt å bruke
     *
     * @return Ufiltrert liste av tilrettelegginger
     */
    public List<SvpTilretteleggingEntitet> getAktuelleTilretteleggingerUfiltrert() {
        if (overstyrteTilrettelegginger != null ) {
            return overstyrteTilrettelegginger.getTilretteleggingListe();
        }
        if (opprinneligeTilrettelegginger != null) {
            return opprinneligeTilrettelegginger.getTilretteleggingListe();
        }
        return Collections.emptyList();
    }

    /**
     * Tar hensyn til valget saksbehandler har gjort om hvorvidt tilretteleggingen skal brukes eller ikke.
     *
     * @return En filtrert liste av tilrettelegginger som skal brukes.
     */
    public List<SvpTilretteleggingEntitet> getAktuelleTilretteleggingerFiltrert() {
        return getAktuelleTilretteleggingerUfiltrert().stream()
            .filter(SvpTilretteleggingEntitet::getSkalBrukes)
            .collect(Collectors.toList());
    }

    public Optional<LocalDate> getFørsteTilretteleggingsbehovdatoFiltrert() {
        return getAktuelleTilretteleggingerFiltrert().stream()
            .map(SvpTilretteleggingEntitet::getBehovForTilretteleggingFom)
            .min(LocalDate::compareTo);
    }

}
