package no.nav.foreldrepenger.behandlingslager.uttak.svp;


import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

public class SvangerskapspengerUttakResultatRepositoryTest {

    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    @Test
    public void skal_kunne_lagre_og_uttak_med_oppfylt_periode() {
        var behandling = opprettBehandling();

        var fom = LocalDate.of(2019, Month.JANUARY, 1);
        var tom = LocalDate.of(2019, Month.MARCH, 31);

        var uttakPeriode = new SvangerskapspengerUttakResultatPeriodeEntitet.Builder(fom, tom)
            .medRegelInput("{}")
            .medRegelEvaluering("{}")
            .medUtbetalingsgrad(BigDecimal.valueOf(100L))
            .medPeriodeIkkeOppfyltÅrsak(PeriodeIkkeOppfyltÅrsak.INGEN)
            .medPeriodeResultatType(PeriodeResultatType.INNVILGET)
            .build();

        AktørId aktørId = AktørId.dummy();
        var uttakArbeidsforhold = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder()
            .medArbeidsforhold(Arbeidsgiver.person(aktørId), null)
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medPeriode(uttakPeriode)
            .build();
        var uttakResultat= new SvangerskapspengerUttakResultatEntitet.Builder(behandling.getBehandlingsresultat()).medUttakResultatArbeidsforhold(uttakArbeidsforhold).build();
        repositoryProvider.getSvangerskapspengerUttakResultatRepository().lagre(behandling.getId(), uttakResultat);

        repoRule.getEntityManager().flush();
        repoRule.getEntityManager().clear();

        var hentetUttaksresultat = repositoryProvider.getSvangerskapspengerUttakResultatRepository().hentHvisEksisterer(behandling.getId());

        assertThat(hentetUttaksresultat).isPresent();
        var arbeidsforholdListe = hentetUttaksresultat.get().getUttaksResultatArbeidsforhold();
        assertThat(arbeidsforholdListe).hasSize(1);
        var arbeidsforhold = arbeidsforholdListe.get(0);
        assertThat(arbeidsforhold.getArbeidsforholdIkkeOppfyltÅrsak()).isEqualTo(ArbeidsforholdIkkeOppfyltÅrsak.INGEN);
        assertThat(arbeidsforhold.getArbeidsgiver().getErVirksomhet()).isFalse();
        assertThat(arbeidsforhold.getArbeidsgiver().getAktørId()).isEqualTo(aktørId);
        var perioder = arbeidsforhold.getPerioder();
        assertThat(perioder).hasSize(1);
        assertThat(perioder.get(0).getTidsperiode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        assertThat(perioder.get(0).getUtbetalingsgrad()).isEqualTo(BigDecimal.valueOf(100L));
        assertThat(perioder.get(0).getPeriodeResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);
    }



    @Test
    public void skal_kunne_lagre_og_uttak_med_ikke_oppfylt_periode() {
        var behandling = opprettBehandling();

        var fom = LocalDate.of(2019, Month.JANUARY, 1);
        var tom = LocalDate.of(2019, Month.MARCH, 31);

        var uttakPeriode = new SvangerskapspengerUttakResultatPeriodeEntitet.Builder(fom, tom)
            .medRegelInput("{}")
            .medRegelEvaluering("{}")
            .medUtbetalingsgrad(BigDecimal.valueOf(100L))
            .medPeriodeIkkeOppfyltÅrsak(PeriodeIkkeOppfyltÅrsak._8308_SØKT_FOR_SENT)
            .medPeriodeResultatType(PeriodeResultatType.AVSLÅTT)
            .build();

        var uttakArbeidsforhold = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder()
            .medArbeidsforhold(Arbeidsgiver.person(AktørId.dummy()), null)
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medPeriode(uttakPeriode)
            .build();
        var uttakResultat= new SvangerskapspengerUttakResultatEntitet.Builder(behandling.getBehandlingsresultat()).medUttakResultatArbeidsforhold(uttakArbeidsforhold).build();
        repositoryProvider.getSvangerskapspengerUttakResultatRepository().lagre(behandling.getId(), uttakResultat);

        repoRule.getEntityManager().flush();
        repoRule.getEntityManager().clear();

        var hentetUttaksresultat = repositoryProvider.getSvangerskapspengerUttakResultatRepository().hentHvisEksisterer(behandling.getId());

        assertThat(hentetUttaksresultat).isPresent();
        var arbeidsforhold = hentetUttaksresultat.get().getUttaksResultatArbeidsforhold();
        assertThat(arbeidsforhold).hasSize(1);
        assertThat(arbeidsforhold.get(0).getArbeidsforholdIkkeOppfyltÅrsak()).isEqualTo(ArbeidsforholdIkkeOppfyltÅrsak.INGEN);
        var perioder = arbeidsforhold.get(0).getPerioder();
        assertThat(perioder).hasSize(1);
        assertThat(perioder.get(0).getTidsperiode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        assertThat(perioder.get(0).getUtbetalingsgrad()).isEqualTo(BigDecimal.valueOf(100L));
        assertThat(perioder.get(0).getPeriodeResultatType()).isEqualTo(PeriodeResultatType.AVSLÅTT);
        assertThat(perioder.get(0).getPeriodeIkkeOppfyltÅrsak()).isEqualTo(PeriodeIkkeOppfyltÅrsak._8308_SØKT_FOR_SENT);
    }


    @Test
    public void skal_kunne_lagre_og_uttak_med_ikke_oppfylt_arbeidsforhold() {
        var behandling = opprettBehandling();

        var uttakArbeidsforhold = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder()
            .medArbeidsforhold(Arbeidsgiver.person(AktørId.dummy()), null)
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medArbeidsforholdIkkeOppfyltÅrsak(ArbeidsforholdIkkeOppfyltÅrsak.UTTAK_KUN_PÅ_HELG)
            .build();
        var uttakResultat= new SvangerskapspengerUttakResultatEntitet.Builder(behandling.getBehandlingsresultat()).medUttakResultatArbeidsforhold(uttakArbeidsforhold).build();
        repositoryProvider.getSvangerskapspengerUttakResultatRepository().lagre(behandling.getId(), uttakResultat);

        repoRule.getEntityManager().flush();
        repoRule.getEntityManager().clear();

        var hentetUttaksresultat = repositoryProvider.getSvangerskapspengerUttakResultatRepository().hentHvisEksisterer(behandling.getId());

        assertThat(hentetUttaksresultat).isPresent();
        var arbeidsforhold = hentetUttaksresultat.get().getUttaksResultatArbeidsforhold();
        assertThat(arbeidsforhold).hasSize(1);
        assertThat(arbeidsforhold.get(0).getArbeidsforholdIkkeOppfyltÅrsak()).isEqualTo(ArbeidsforholdIkkeOppfyltÅrsak.UTTAK_KUN_PÅ_HELG);
        var perioder = arbeidsforhold.get(0).getPerioder();
        assertThat(perioder).hasSize(0);
    }

    private Behandling opprettBehandling() {
        var behandling = new BasicBehandlingBuilder(repoRule.getEntityManager()).opprettOgLagreFørstegangssøknad(FagsakYtelseType.SVANGERSKAPSPENGER);
        var behandlingsresultat = Behandlingsresultat.opprettFor(behandling);
        new BehandlingsresultatRepository(repoRule.getEntityManager()).lagre(behandling.getId(), behandlingsresultat);
        return behandling;
    }
}
