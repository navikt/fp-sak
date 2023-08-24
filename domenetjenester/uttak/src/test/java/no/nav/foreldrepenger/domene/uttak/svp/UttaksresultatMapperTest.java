package no.nav.foreldrepenger.domene.uttak.svp;

import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.ArbeidsforholdIkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.PeriodeIkkeOppfyltÅrsak;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;
import no.nav.svangerskapspenger.domene.felles.AktivitetType;
import no.nav.svangerskapspenger.domene.felles.Arbeidsforhold;
import no.nav.svangerskapspenger.domene.resultat.Uttaksperiode;
import no.nav.svangerskapspenger.domene.resultat.Uttaksperioder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;

class UttaksresultatMapperTest {

    @Test
    void mapping_av_regelmodell() {
        var aktørId = AktørId.dummy();
        var internRef = InternArbeidsforholdRef.nyRef().getReferanse();

        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        var repositoryProvider = new UttakRepositoryStubProvider();
        var behandling = scenario.lagre(repositoryProvider);
        var perioder = new Uttaksperioder();
        var periode = new Uttaksperiode(LocalDate.of(2019, Month.JANUARY, 1), LocalDate.of(2019, Month.MARCH, 31), BigDecimal.ZERO);
        periode.avslå(no.nav.svangerskapspenger.domene.resultat.PeriodeIkkeOppfyltÅrsak.SØKT_FOR_SENT, "", "");
        perioder.leggTilPerioder(Arbeidsforhold.aktør(AktivitetType.ARBEID, aktørId.getId(), internRef), periode);

        var behandlingsresultat = repositoryProvider.getBehandlingsresultatRepository().hent(behandling.getId());
        var uttaksresultatMapper = new UttaksresultatMapper();
        var uttakResultatEntitet = uttaksresultatMapper.tilEntiteter(behandlingsresultat, perioder);

        assertThat(uttakResultatEntitet).isNotNull();
        assertThat(uttakResultatEntitet.getUttaksResultatArbeidsforhold()).hasSize(1);
        var arbeidsforholdEntitet = uttakResultatEntitet.getUttaksResultatArbeidsforhold().get(0);
        assertThat(arbeidsforholdEntitet.getArbeidsforholdIkkeOppfyltÅrsak()).isEqualTo(ArbeidsforholdIkkeOppfyltÅrsak.INGEN);
        assertThat(arbeidsforholdEntitet.getPerioder()).hasSize(1);
        var periodeEntitet = arbeidsforholdEntitet.getPerioder().get(0);
        assertThat(periodeEntitet.getPeriodeIkkeOppfyltÅrsak()).isEqualTo(PeriodeIkkeOppfyltÅrsak._8308_SØKT_FOR_SENT);
        assertThat(periodeEntitet.getFom()).isEqualTo(periode.getFom());
        assertThat(periodeEntitet.getTom()).isEqualTo(periode.getTom());
        assertThat(periodeEntitet.getPeriodeResultatType()).isEqualTo(PeriodeResultatType.AVSLÅTT);
        assertThat(periodeEntitet.getUtbetalingsgrad()).isEqualTo(Utbetalingsgrad.ZERO);
    }
}
