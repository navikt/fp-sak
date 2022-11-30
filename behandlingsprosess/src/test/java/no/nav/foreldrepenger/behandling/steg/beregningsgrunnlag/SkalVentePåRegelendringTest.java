package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.folketrygdloven.kalkulator.modell.opptjening.OpptjeningAktiviteterDto;
import no.nav.folketrygdloven.kalkulator.tid.Intervall;
import no.nav.folketrygdloven.kalkulus.kodeverk.OpptjeningAktivitetType;

class SkalVentePåRegelendringTest {
    private static final LocalDate REGELENDRING = LocalDate.of(2023,1,1);

    @Test
    public void skalIkkeSettePåVentMedTidligSTP() {
        var arbeid = OpptjeningAktiviteterDto.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID,
            Intervall.fraOgMedTilOgMed(førEndring(300), førEndring(10)), "999999999");
        var næring = OpptjeningAktiviteterDto.nyPeriode(OpptjeningAktivitetType.NÆRING,
            Intervall.fraOgMedTilOgMed(førEndring(300), etterEndring(10)));
        var opptjening = new OpptjeningAktiviteterDto(arbeid, næring);
        var skalVente = SkalVentePåRegelendring.kanPåvirkesAvRegelendring(førEndring(50), opptjening);
        assertThat(skalVente).isFalse();
    }

    @Test
    public void skalIkkeVenteNårIngenRelevantStatus() {
        var dagpenger = OpptjeningAktiviteterDto.nyPeriode(OpptjeningAktivitetType.DAGPENGER,
            Intervall.fraOgMedTilOgMed(førEndring(300), etterEndring(100)));
        var opptjening = new OpptjeningAktiviteterDto(dagpenger);
        var skalVente = SkalVentePåRegelendring.kanPåvirkesAvRegelendring(etterEndring(10), opptjening);
        assertThat(skalVente).isFalse();
    }

    @Test
    public void skalIkkeVenteNårIngenAktivitetFremTilSTP() {
        var arbeid = OpptjeningAktiviteterDto.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID,
            Intervall.fraOgMedTilOgMed(førEndring(300), førEndring(10)), "999999999");
        var næring = OpptjeningAktiviteterDto.nyPeriode(OpptjeningAktivitetType.NÆRING,
            Intervall.fraOgMedTilOgMed(førEndring(300), førEndring(10)));
        var opptjening = new OpptjeningAktiviteterDto(arbeid, næring);
        var skalVente = SkalVentePåRegelendring.kanPåvirkesAvRegelendring(etterEndring(10), opptjening);
        assertThat(skalVente).isFalse();
    }

    @Test
    public void skalVenteNårNæringOgArbeid() {
        var arbeid = OpptjeningAktiviteterDto.nyPeriodeOrgnr(OpptjeningAktivitetType.ARBEID,
            Intervall.fraOgMedTilOgMed(førEndring(300), etterEndring(10)), "999999999");
        var næring = OpptjeningAktiviteterDto.nyPeriode(OpptjeningAktivitetType.NÆRING,
            Intervall.fraOgMedTilOgMed(førEndring(300), etterEndring(10)));
        var opptjening = new OpptjeningAktiviteterDto(arbeid, næring);
        var skalVente = SkalVentePåRegelendring.kanPåvirkesAvRegelendring(etterEndring(10), opptjening);
        assertThat(skalVente).isTrue();
    }

    @Test
    public void skalVenteNårNæringOgFrilans() {
        var frilans = OpptjeningAktiviteterDto.nyPeriode(OpptjeningAktivitetType.FRILANS,
            Intervall.fraOgMedTilOgMed(førEndring(300), etterEndring(10)));
        var næring = OpptjeningAktiviteterDto.nyPeriode(OpptjeningAktivitetType.NÆRING,
            Intervall.fraOgMedTilOgMed(førEndring(300), etterEndring(10)));
        var opptjening = new OpptjeningAktiviteterDto(frilans, næring);
        var skalVente = SkalVentePåRegelendring.kanPåvirkesAvRegelendring(etterEndring(10), opptjening);
        assertThat(skalVente).isTrue();
    }

    @Test
    public void skalIkkeVenteNårKunNæring() {
        var næring = OpptjeningAktiviteterDto.nyPeriode(OpptjeningAktivitetType.NÆRING,
            Intervall.fraOgMedTilOgMed(førEndring(300), etterEndring(10)));
        var opptjening = new OpptjeningAktiviteterDto(næring);
        var skalVente = SkalVentePåRegelendring.kanPåvirkesAvRegelendring(etterEndring(10), opptjening);
        assertThat(skalVente).isFalse();
    }

    private LocalDate førEndring(int dager) {
        return REGELENDRING.minusDays(dager);
    }

    private LocalDate etterEndring(int dager) {
        return REGELENDRING.plusMonths(dager);
    }
}
