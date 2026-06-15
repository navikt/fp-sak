package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;


public record TilretteleggingEndring(EndringType endringType,
                                     SvpTilretteleggingEntitet nyTilrettelegging,
                                     List<SvpTilretteleggingEntitet> gammelTilrettelegging) {

    public static TilretteleggingEndring splitt(SvpTilretteleggingEntitet splittesTil) {
        return new TilretteleggingEndring(EndringType.SPLITT, splittesTil, List.of());
    }

    public static TilretteleggingEndring reverserSplitt(SvpTilretteleggingEntitet reverseresTil, List<SvpTilretteleggingEntitet> reverseresFra) {
        return new TilretteleggingEndring(EndringType.REVERSER_SPLITT, reverseresTil, reverseresFra);
    }

    public static TilretteleggingEndring endret(SvpTilretteleggingEntitet nyTilrettelegging, SvpTilretteleggingEntitet gammelTilrettelegging) {
        return new TilretteleggingEndring(EndringType.ENDRET, nyTilrettelegging, List.of(gammelTilrettelegging));
    }

    public static TilretteleggingEndring uendret(SvpTilretteleggingEntitet uendretTilrettelegging) {
        return new TilretteleggingEndring(EndringType.UENDRET, uendretTilrettelegging, List.of(uendretTilrettelegging));
    }

    public boolean skalOppdateres() {
        return this.endringType != EndringType.UENDRET;
    }

    public boolean erNy() {
        return this.endringType == EndringType.SPLITT || this.endringType == EndringType.REVERSER_SPLITT;
    }

    public Optional<SvpTilretteleggingEntitet> getEndretFra() {
        return this.erNy() ? Optional.empty() : Optional.of(gammelTilrettelegging().getFirst());
    }

    enum EndringType {
        SPLITT,
        REVERSER_SPLITT,
        ENDRET,
        UENDRET
    }
}
