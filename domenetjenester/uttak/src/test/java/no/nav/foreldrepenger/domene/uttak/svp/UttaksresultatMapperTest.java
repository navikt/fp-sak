package no.nav.foreldrepenger.domene.uttak.svp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.ArbeidsforholdIkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.PeriodeIkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatArbeidsforholdEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.svangerskapspenger.domene.felles.AktivitetType;
import no.nav.svangerskapspenger.domene.felles.Arbeidsforhold;
import no.nav.svangerskapspenger.domene.resultat.Uttaksperiode;
import no.nav.svangerskapspenger.domene.resultat.Uttaksperioder;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

public class UttaksresultatMapperTest {

    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();
    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryProvider(repoRule.getEntityManager());
    private final UttaksresultatMapper uttaksresultatMapper = new UttaksresultatMapper();

    @Test
    public void mapping_av_regelmodell() {
        AktørId aktørId = AktørId.dummy();
        String internRef = InternArbeidsforholdRef.nyRef().getReferanse();

        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();
        var behandling = scenario.lagre(repositoryProvider);
        var perioder = new Uttaksperioder();
        var periode = new Uttaksperiode(LocalDate.of(2019, Month.JANUARY, 1), LocalDate.of(2019, Month.MARCH, 31), BigDecimal.ZERO);
        periode.avslå(no.nav.svangerskapspenger.domene.resultat.PeriodeIkkeOppfyltÅrsak.SØKT_FOR_SENT, "", "");
        perioder.leggTilPerioder(Arbeidsforhold.aktør(AktivitetType.ARBEID, aktørId.getId(), internRef), periode);

        var uttakResultatEntitet = uttaksresultatMapper.tilEntiteter(behandling.getBehandlingsresultat(), perioder);

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
        assertThat(periodeEntitet.getUtbetalingsgrad()).isEqualTo(BigDecimal.ZERO);
    }


    @Test
    public void mapping_av_entiteter() {
        var behandlingsresultatMock = mock(Behandlingsresultat.class);
        var fom = LocalDate.of(2019, Month.JANUARY, 1);
        var tom = LocalDate.of(2019, Month.JANUARY, 31);

        var periodeEntitet = new SvangerskapspengerUttakResultatPeriodeEntitet.Builder(fom, tom)
            .medPeriodeResultatType(PeriodeResultatType.INNVILGET)
            .medUtbetalingsgrad(BigDecimal.valueOf(50L))
            .build();
        var arbeidsforholdEntitet = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder().medArbeidsforhold(Arbeidsgiver.fra(AktørId.dummy()), null).medPeriode(periodeEntitet).build();
        var uttaksresultatEntitet = new SvangerskapspengerUttakResultatEntitet.Builder(behandlingsresultatMock).medUttakResultatArbeidsforhold(arbeidsforholdEntitet).build();

        var uttaksperioder = uttaksresultatMapper.tilRegelmodell(uttaksresultatEntitet);

        assertThat(uttaksperioder).isNotNull();
        assertThat(uttaksperioder.alleArbeidsforhold()).hasSize(1);
        var arbeidsforhold = uttaksperioder.alleArbeidsforhold().iterator().next();
        assertThat(uttaksperioder.perioder(arbeidsforhold).getUttaksperioder()).hasSize(1);
        var periode = uttaksperioder.perioder(arbeidsforhold).getUttaksperioder().get(0);
        assertThat(periode.getFom()).isEqualTo(fom);
        assertThat(periode.getTom()).isEqualTo(tom);
        assertThat(periode.getUtbetalingsgrad()).isEqualTo(BigDecimal.valueOf(50L));
    }

}
