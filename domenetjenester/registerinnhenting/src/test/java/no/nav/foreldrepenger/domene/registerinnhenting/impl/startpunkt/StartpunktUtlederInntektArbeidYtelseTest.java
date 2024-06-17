package no.nav.foreldrepenger.domene.registerinnhenting.impl.startpunkt;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.arbeidsforhold.IAYGrunnlagDiff;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.VirksomhetType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

class StartpunktUtlederInntektArbeidYtelseTest {

    @Test
    void diff_oppgitt_opptjening() {
        var næring1 = OppgittOpptjeningBuilder.EgenNæringBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMed(LocalDate.now().minusYears(1)))
            .medVirksomhet(OrgNummer.KUNSTIG_ORG)
            .medVirksomhetType(VirksomhetType.ANNEN);
        var oppgitt1 = OppgittOpptjeningBuilder.ny().leggTilEgenNæring(næring1.build());

        var næring2 = OppgittOpptjeningBuilder.EgenNæringBuilder.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMed(LocalDate.now().minusYears(1)))
            .medVirksomhet(OrgNummer.KUNSTIG_ORG)
            .medVirksomhetType(VirksomhetType.ANNEN)
            .medNyoppstartet(true)
            .medBruttoInntekt(new BigDecimal(500000));
        var oppgitt2 = OppgittOpptjeningBuilder.ny().leggTilEgenNæring(næring2.build());


        assertThat(IAYGrunnlagDiff.erEndringPåOppgittOpptjening(Optional.of(oppgitt1.build()), Optional.of(oppgitt2.build()))).isTrue();
        assertThat(IAYGrunnlagDiff.erEndringPåOppgittOpptjening(Optional.of(oppgitt2.build()), Optional.of(oppgitt2.build()))).isFalse();
    }

}
