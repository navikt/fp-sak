package no.nav.foreldrepenger.behandlingslager.uttak.svp;


import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.BasicBehandlingBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;

class SvangerskapspengerUttakResultatRepositoryTest extends EntityManagerAwareTest {

    private BehandlingsresultatRepository behandlingsresultatRepository;
    private SvangerskapspengerUttakResultatRepository svangerskapspengerUttakResultatRepository;

    @BeforeEach
    public void setUp() {
        var entityManager = getEntityManager();
        behandlingsresultatRepository = new BehandlingsresultatRepository(entityManager);
        svangerskapspengerUttakResultatRepository = new SvangerskapspengerUttakResultatRepository(entityManager);

    }

    @Test
    void skal_kunne_lagre_og_uttak_med_oppfylt_periode() {
        var behandling = opprettBehandling();

        var fom = LocalDate.of(2019, Month.JANUARY, 1);
        var tom = LocalDate.of(2019, Month.MARCH, 31);

        var uttakPeriode = new SvangerskapspengerUttakResultatPeriodeEntitet.Builder(fom, tom)
            .medRegelInput("{}")
            .medRegelEvaluering("{}")
            .medUtbetalingsgrad(Utbetalingsgrad.FULL)
            .medPeriodeIkkeOppfyltÅrsak(PeriodeIkkeOppfyltÅrsak.INGEN)
            .medPeriodeResultatType(PeriodeResultatType.INNVILGET)
            .build();

        var aktørId = AktørId.dummy();
        var uttakArbeidsforhold = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder()
            .medArbeidsforhold(Arbeidsgiver.person(aktørId), null)
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medPeriode(uttakPeriode)
            .build();
        var br = behandlingsresultatRepository.hent(behandling.getId());
        var uttakResultat= new SvangerskapspengerUttakResultatEntitet.Builder(br)
            .medUttakResultatArbeidsforhold(uttakArbeidsforhold).build();
        svangerskapspengerUttakResultatRepository.lagre(behandling.getId(), uttakResultat);

        var hentetUttaksresultat = svangerskapspengerUttakResultatRepository.hentHvisEksisterer(behandling.getId());

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
        assertThat(perioder.get(0).getUtbetalingsgrad()).isEqualTo(Utbetalingsgrad.HUNDRED);
        assertThat(perioder.get(0).getPeriodeResultatType()).isEqualTo(PeriodeResultatType.INNVILGET);
    }

    @Test
    void skal_kunne_lagre_og_uttak_med_ikke_oppfylt_periode() {
        var behandling = opprettBehandling();

        var fom = LocalDate.of(2019, Month.JANUARY, 1);
        var tom = LocalDate.of(2019, Month.MARCH, 31);

        var uttakPeriode = new SvangerskapspengerUttakResultatPeriodeEntitet.Builder(fom, tom)
            .medRegelInput("{}")
            .medRegelEvaluering("{}")
            .medUtbetalingsgrad(Utbetalingsgrad.FULL)
            .medPeriodeIkkeOppfyltÅrsak(PeriodeIkkeOppfyltÅrsak._8308_SØKT_FOR_SENT)
            .medPeriodeResultatType(PeriodeResultatType.AVSLÅTT)
            .build();

        var uttakArbeidsforhold = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder()
            .medArbeidsforhold(Arbeidsgiver.person(AktørId.dummy()), null)
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medPeriode(uttakPeriode)
            .build();
        var br = behandlingsresultatRepository.hent(behandling.getId());
        var uttakResultat= new SvangerskapspengerUttakResultatEntitet.Builder(br).medUttakResultatArbeidsforhold(uttakArbeidsforhold).build();
        svangerskapspengerUttakResultatRepository.lagre(behandling.getId(), uttakResultat);

        var hentetUttaksresultat = svangerskapspengerUttakResultatRepository.hentHvisEksisterer(behandling.getId());

        assertThat(hentetUttaksresultat).isPresent();
        var arbeidsforhold = hentetUttaksresultat.get().getUttaksResultatArbeidsforhold();
        assertThat(arbeidsforhold).hasSize(1);
        assertThat(arbeidsforhold.get(0).getArbeidsforholdIkkeOppfyltÅrsak()).isEqualTo(ArbeidsforholdIkkeOppfyltÅrsak.INGEN);
        var perioder = arbeidsforhold.get(0).getPerioder();
        assertThat(perioder).hasSize(1);
        assertThat(perioder.get(0).getTidsperiode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        assertThat(perioder.get(0).getUtbetalingsgrad()).isEqualTo(Utbetalingsgrad.HUNDRED);
        assertThat(perioder.get(0).getPeriodeResultatType()).isEqualTo(PeriodeResultatType.AVSLÅTT);
        assertThat(perioder.get(0).getPeriodeIkkeOppfyltÅrsak()).isEqualTo(PeriodeIkkeOppfyltÅrsak._8308_SØKT_FOR_SENT);
    }

    @Test
    void skal_kunne_lagre_og_uttak_med_ikke_oppfylt_arbeidsforhold() {
        var behandling = opprettBehandling();

        var uttakArbeidsforhold = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder()
            .medArbeidsforhold(Arbeidsgiver.person(AktørId.dummy()), null)
            .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
            .medArbeidsforholdIkkeOppfyltÅrsak(ArbeidsforholdIkkeOppfyltÅrsak.UTTAK_KUN_PÅ_HELG)
            .build();
        var br = behandlingsresultatRepository.hent(behandling.getId());
        var uttakResultat= new SvangerskapspengerUttakResultatEntitet.Builder(br).medUttakResultatArbeidsforhold(uttakArbeidsforhold).build();
        svangerskapspengerUttakResultatRepository.lagre(behandling.getId(), uttakResultat);


        var hentetUttaksresultat = svangerskapspengerUttakResultatRepository.hentHvisEksisterer(behandling.getId());

        assertThat(hentetUttaksresultat).isPresent();
        var arbeidsforhold = hentetUttaksresultat.get().getUttaksResultatArbeidsforhold();
        assertThat(arbeidsforhold).hasSize(1);
        assertThat(arbeidsforhold.get(0).getArbeidsforholdIkkeOppfyltÅrsak()).isEqualTo(ArbeidsforholdIkkeOppfyltÅrsak.UTTAK_KUN_PÅ_HELG);
        var perioder = arbeidsforhold.get(0).getPerioder();
        assertThat(perioder).hasSize(0);
    }

    private Behandling opprettBehandling() {
        var entityManager = getEntityManager();
        var behandling = new BasicBehandlingBuilder(entityManager).opprettOgLagreFørstegangssøknad(FagsakYtelseType.SVANGERSKAPSPENGER);
        var behandlingsresultat = Behandlingsresultat.opprettFor(behandling);
        new BehandlingsresultatRepository(entityManager).lagre(behandling.getId(), behandlingsresultat);
        return behandling;
    }
}
