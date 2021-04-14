package no.nav.foreldrepenger.domene.abakus.mapping;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.KUNSTIG_ORG;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.abakus.iaygrunnlag.Organisasjon;
import no.nav.abakus.iaygrunnlag.inntektsmelding.v1.RefusjonskravDatoDto;
import no.nav.abakus.iaygrunnlag.inntektsmelding.v1.RefusjonskravDatoerDto;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;

public class MapRefusjonskravDatoerTest {

    @Test
    public void skal_mappe_refusjonskravdato() {
        // Arrange
        var førsteInnsendingAvRefusjonskrav = LocalDate.now().minusDays(10);
        var førsteDagMedRefusjonskrav = LocalDate.now().minusDays(20);
        var orgnr = KUNSTIG_ORG;
        var dto = new RefusjonskravDatoerDto(List.of(new RefusjonskravDatoDto(new Organisasjon(orgnr),
                førsteInnsendingAvRefusjonskrav, førsteDagMedRefusjonskrav, true)));

        // Act
        var refusjonskravDatoer = MapRefusjonskravDatoer.map(dto);

        // Assert
        assertThat(refusjonskravDatoer).hasSize(1);
        assertThat(refusjonskravDatoer.get(0).getArbeidsgiver()).isEqualTo(Arbeidsgiver.virksomhet(orgnr));
        assertThat(refusjonskravDatoer.get(0).getFørsteDagMedRefusjonskrav().get()).isEqualTo(førsteDagMedRefusjonskrav);
        assertThat(refusjonskravDatoer.get(0).getFørsteInnsendingAvRefusjonskrav()).isEqualTo(førsteInnsendingAvRefusjonskrav);
        assertThat(refusjonskravDatoer.get(0).harRefusjonFraStart()).isTrue();

    }
}
