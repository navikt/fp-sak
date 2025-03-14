package no.nav.foreldrepenger.domene.registerinnhenting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.RelatertBehandlingTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravArbeidPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravArbeidPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravArbeidRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitetskrav.AktivitetskravPermisjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.abakus.ArbeidsforholdTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdMedPermisjon;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;

@ExtendWith(MockitoExtension.class)
class MorsAktivitetInnhenterTest {

    private MorsAktivitetInnhenter morsAktivitetInnhenter;

    @Mock
    private YtelsesFordelingRepository ytelseFordelingTjeneste;
    @Mock
    private RelatertBehandlingTjeneste relatertBehandlingTjeneste;
    @Mock
    private PersonopplysningTjeneste personopplysningTjeneste;
    @Mock
    private ArbeidsforholdTjeneste abakusArbeidsforholdTjeneste;
    @Mock
    private AktivitetskravArbeidRepository aktivitetskravArbeidRepository;


    @BeforeEach
    void setUp() {
        morsAktivitetInnhenter = new MorsAktivitetInnhenter(ytelseFordelingTjeneste, relatertBehandlingTjeneste, personopplysningTjeneste,
            abakusArbeidsforholdTjeneste, aktivitetskravArbeidRepository);
    }

    @Test
    void sjekk_at_mor_har_riktig_aktivitetskrav_info_første_gang() {
        var annenPartAktørId = AktørId.dummy();
        var fraDato = LocalDate.now().minusWeeks(1);
        var tilDato = LocalDate.now().plusWeeks(4);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fraDato, tilDato);
        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        var behandling = førstegangScenario.lagMocked();
        var perioderMedAktivitetskrav = List.of(lagOppgittPeriode(LocalDate.now().minusWeeks(1), LocalDate.now()),
            lagOppgittPeriode(LocalDate.now().plusWeeks(2), LocalDate.now().plusWeeks(4)));
        var aktivitetsavtaler = List.of(lagAktivitetsavtale(periode, BigDecimal.valueOf(100)));

        var arbeidsforholdMedPermisjon = List.of(
            lagArbeidsforholdMedPermisjon(Arbeidsgiver.virksomhet("999999999"), EksternArbeidsforholdRef.ref("01"), aktivitetsavtaler,
                Collections.emptyList()));

        when(abakusArbeidsforholdTjeneste.hentArbeidsforholdInfoForEnPeriode(any(), any(), any(), any())).thenReturn(arbeidsforholdMedPermisjon);

        var morAktivitet = morsAktivitetInnhenter.finnMorsAktivitet(behandling, perioderMedAktivitetskrav, annenPartAktørId, Optional.empty())
            .orElseThrow();
        var aktivitetPeriodeEntitetListe = morAktivitet.perioderEntitet().getAktivitetskravArbeidPeriodeListe();


        assertThat(morAktivitet.fraDato()).isEqualTo(fraDato.minusWeeks(2));
        assertThat(morAktivitet.tilDato()).isEqualTo(tilDato.plusWeeks(2));
        assertThat(aktivitetPeriodeEntitetListe).hasSize(3);
    }

    @Test
    void sjekk_at_mor_har_samme_aktivitet_i_hele_perioden() {
        var annenPartAktørId = AktørId.dummy();
        var fraDato = LocalDate.now().minusWeeks(1);
        var tilDato = LocalDate.now().plusWeeks(4);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fraDato.minusWeeks(2), tilDato.plusWeeks(2));
        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        var behandling = førstegangScenario.lagMocked();
        var virksomhet = Arbeidsgiver.virksomhet("999999999");
        var perioderMedAktivitetskrav = List.of(lagOppgittPeriode(LocalDate.now().minusWeeks(1), LocalDate.now()),
            lagOppgittPeriode(LocalDate.now().plusWeeks(2), LocalDate.now().plusWeeks(4)));
        var aktivitetsavtaler = List.of(lagAktivitetsavtale(periode, BigDecimal.valueOf(100)));

        var arbeidsforholdMedPermisjon = List.of(
            lagArbeidsforholdMedPermisjon(virksomhet, EksternArbeidsforholdRef.ref("01"), aktivitetsavtaler, Collections.emptyList()));

        when(abakusArbeidsforholdTjeneste.hentArbeidsforholdInfoForEnPeriode(any(), any(), any(), any())).thenReturn(arbeidsforholdMedPermisjon);

        var morAktivitet = morsAktivitetInnhenter.finnMorsAktivitet(behandling, perioderMedAktivitetskrav, annenPartAktørId, Optional.empty())
            .orElseThrow();
        var aktivitetPeriodeEntitetListe = morAktivitet.perioderEntitet().getAktivitetskravArbeidPeriodeListe();


        assertThat(morAktivitet.fraDato()).isEqualTo(fraDato.minusWeeks(2));
        assertThat(morAktivitet.tilDato()).isEqualTo(tilDato.plusWeeks(2));
        assertThat(aktivitetPeriodeEntitetListe).hasSize(1);
        assertThat(aktivitetPeriodeEntitetListe.stream()
            .map(aktPeriode -> aktPeriode.getSumStillingsprosent().getVerdi())
            .reduce(BigDecimal::add)
            .orElse(BigDecimal.ZERO)).isEqualTo(BigDecimal.valueOf(100));
    }

    @Test
    void sjekk_at_permisjon_settes_til_flere_hvis_flere_typer_permisjon_i_samme_periode() {
        var annenPartAktørId = AktørId.dummy();
        var fraDato = LocalDate.now().minusWeeks(1);
        var tilDato = LocalDate.now().plusWeeks(4);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fraDato.minusWeeks(2), tilDato.plusWeeks(2));
        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        var behandling = førstegangScenario.lagMocked();
        var virksomhet = Arbeidsgiver.virksomhet("999999999");
        var perioderMedAktivitetskrav = List.of(lagOppgittPeriode(LocalDate.now().minusWeeks(1), LocalDate.now()),
            lagOppgittPeriode(LocalDate.now().plusWeeks(2), LocalDate.now().plusWeeks(4)));
        var aktivitetsavtaler = List.of(lagAktivitetsavtale(periode, BigDecimal.valueOf(100)));

        var permisjoner = List.of(lagPermisjon(periode, BigDecimal.TEN), lagPermisjon(periode, BigDecimal.TEN));
        var arbeidsforholdMedPermisjon = List.of(
            lagArbeidsforholdMedPermisjon(virksomhet, EksternArbeidsforholdRef.ref("01"), aktivitetsavtaler, permisjoner));

        when(abakusArbeidsforholdTjeneste.hentArbeidsforholdInfoForEnPeriode(any(), any(), any(), any())).thenReturn(arbeidsforholdMedPermisjon);

        var morAktivitet = morsAktivitetInnhenter.finnMorsAktivitet(behandling, perioderMedAktivitetskrav, annenPartAktørId, Optional.empty())
            .orElseThrow();
        var aktivitetPeriodeEntitetListe = morAktivitet.perioderEntitet().getAktivitetskravArbeidPeriodeListe();


        assertThat(morAktivitet.fraDato()).isEqualTo(fraDato.minusWeeks(2));
        assertThat(morAktivitet.tilDato()).isEqualTo(tilDato.plusWeeks(2));
        assertThat(aktivitetPeriodeEntitetListe).hasSize(1);
        var arbeidPeriode = aktivitetPeriodeEntitetListe.getFirst()
            .getAktivitetskravArbeidPerioder()
            .getAktivitetskravArbeidPeriodeListe()
            .getFirst();
        assertThat(arbeidPeriode.getSumPermisjonsprosent().getVerdi()).isEqualTo(BigDecimal.valueOf(20));
        assertThat(arbeidPeriode.getPermisjonsbeskrivelseType()).isEqualTo(AktivitetskravPermisjonType.FLERE);
    }

    @Test
    void sjekk_at_forrige_innhentingsfradato_opprettholdes_når_nye_aktivitetskrav_perioder_er_senere() {
        var annenPartAktørId = AktørId.dummy();
        var virksomhet = Arbeidsgiver.virksomhet("999999999");

        //førstegangsbehandling
        var nyfraDato = LocalDate.now();
        var nytilDato = LocalDate.now().plusWeeks(4);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(nyfraDato, nytilDato);
        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        var behandling = førstegangScenario.lagMocked();
        var perioderMedAktivitetskrav = List.of(lagOppgittPeriode(nyfraDato, nytilDato));
        var aktivitetsavtaler = List.of(lagAktivitetsavtale(periode, BigDecimal.valueOf(100)));

        //forrige aktivitetsgrunnlag
        var grunnlagsFraDato = LocalDate.now().minusWeeks(4);
        var grunnlagsTilDato = LocalDate.now();
        var perioder = List.of(lagAktvitetskravArbeidPeriode(grunnlagsFraDato, grunnlagsTilDato, BigDecimal.valueOf(100), virksomhet.getOrgnr()));
        var aktvitetskravPerioder = lagPerioderBuilder(perioder);
        var gjeldendeAktivitetsgrunnlag = lagAktivitetsgrunnlag(behandling.getId(), grunnlagsFraDato, grunnlagsTilDato, aktvitetskravPerioder);

        var arbeidsforholdMedPermisjon = List.of(
            lagArbeidsforholdMedPermisjon(virksomhet, EksternArbeidsforholdRef.ref("01"), aktivitetsavtaler, Collections.emptyList()));

        when(abakusArbeidsforholdTjeneste.hentArbeidsforholdInfoForEnPeriode(any(), any(), any(), any())).thenReturn(arbeidsforholdMedPermisjon);


        var morAktivitet = morsAktivitetInnhenter.finnMorsAktivitet(behandling, perioderMedAktivitetskrav, annenPartAktørId,
            Optional.of(gjeldendeAktivitetsgrunnlag)).orElseThrow();
        var aktivitetPeriodeEntitetListe = morAktivitet.perioderEntitet().getAktivitetskravArbeidPeriodeListe();


        assertThat(morAktivitet.fraDato()).isEqualTo(grunnlagsFraDato);
        assertThat(morAktivitet.tilDato()).isEqualTo(nytilDato.plusWeeks(2));
        assertThat(aktivitetPeriodeEntitetListe).hasSize(3);
    }

    @Test
    void sjekk_at_fradato_fra_ny_aktivitetskravperiode_velges_når_den_er_før_forrige_grunnlagsfradato() {
        var annenPartAktørId = AktørId.dummy();
        var virksomhet = Arbeidsgiver.virksomhet("999999999");

        //førstegangsbehandling
        var nyfraDato = LocalDate.now().minusWeeks(1);
        var nytilDato = LocalDate.now().plusWeeks(2);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(nyfraDato, nytilDato);
        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        var behandling = førstegangScenario.lagMocked();
        var perioderMedAktivitetskrav = List.of(lagOppgittPeriode(nyfraDato, nytilDato));
        var aktivitetsavtaler = List.of(lagAktivitetsavtale(periode, BigDecimal.valueOf(100)));

        //forrige aktivitetsgrunnlag
        var grunnlagsFraDato = LocalDate.now().plusWeeks(1);
        var grunnlagsTilDato = LocalDate.now().plusWeeks(3);
        var perioder = List.of(lagAktvitetskravArbeidPeriode(grunnlagsFraDato, grunnlagsTilDato, BigDecimal.valueOf(100), virksomhet.getOrgnr()));
        var aktvitetskravPerioder = lagPerioderBuilder(perioder);
        var gjeldendeAktivitetsgrunnlag = lagAktivitetsgrunnlag(behandling.getId(), grunnlagsFraDato, grunnlagsTilDato, aktvitetskravPerioder);

        var arbeidsforholdMedPermisjon = List.of(
            lagArbeidsforholdMedPermisjon(virksomhet, EksternArbeidsforholdRef.ref("01"), aktivitetsavtaler, Collections.emptyList()));

        when(abakusArbeidsforholdTjeneste.hentArbeidsforholdInfoForEnPeriode(any(), any(), any(), any())).thenReturn(arbeidsforholdMedPermisjon);


        var morAktivitet = morsAktivitetInnhenter.finnMorsAktivitet(behandling, perioderMedAktivitetskrav, annenPartAktørId,
            Optional.of(gjeldendeAktivitetsgrunnlag)).orElseThrow();
        var aktivitetPeriodeEntitetListe = morAktivitet.perioderEntitet().getAktivitetskravArbeidPeriodeListe();


        assertThat(morAktivitet.fraDato()).isEqualTo(nyfraDato.minusWeeks(2));
        assertThat(morAktivitet.tilDato()).isEqualTo(nytilDato.plusWeeks(2));
        assertThat(aktivitetPeriodeEntitetListe).hasSize(3);
    }

    @Test
    void sjekk_at_mors_aktivitet_er_riktig_når_flere_arbeidsforhold_med_ulik_stillingsprosent_i_perioden_og_permisjon_i_ett() {
        var annenPartAktørId = AktørId.dummy();
        var orgnr1 = Arbeidsgiver.virksomhet("999999999");
        var orgnr2 = Arbeidsgiver.virksomhet("888888888");
        var innhentingsperiodeFraDato = LocalDate.now().minusWeeks(1);
        var innhentingsperiodeTilDato = LocalDate.now().plusWeeks(4);
        var aktivitetsavtalePeriode = DatoIntervallEntitet.fraOgMedTilOgMed(innhentingsperiodeFraDato.minusWeeks(1),
            innhentingsperiodeTilDato.minusWeeks(2));
        var aktivitetsavtalePeriode2 = DatoIntervallEntitet.fraOgMedTilOgMed(innhentingsperiodeTilDato.minusWeeks(2).plusDays(1),
            innhentingsperiodeTilDato);
        var førstegangScenario = ScenarioMorSøkerForeldrepenger.forFødsel().medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD);
        var behandling = førstegangScenario.lagMocked();
        var perioderMedAktivitetskrav = List.of(lagOppgittPeriode(LocalDate.now().minusWeeks(1), LocalDate.now()),
            lagOppgittPeriode(LocalDate.now().plusWeeks(2), LocalDate.now().plusWeeks(4)));
        var aktivitetsavtaler1 = List.of(lagAktivitetsavtale(aktivitetsavtalePeriode, BigDecimal.valueOf(55)));
        var aktivitetsavtaler2 = List.of(lagAktivitetsavtale(aktivitetsavtalePeriode2, BigDecimal.valueOf(25)));
        var permisjoner = List.of(lagPermisjon(aktivitetsavtalePeriode2, BigDecimal.valueOf(20)));

        var arbeidsforholdMedPermisjon = List.of(
            lagArbeidsforholdMedPermisjon(orgnr1, EksternArbeidsforholdRef.ref("01"), aktivitetsavtaler1, permisjoner),
            lagArbeidsforholdMedPermisjon(orgnr2, EksternArbeidsforholdRef.nullRef(), aktivitetsavtaler2, Collections.emptyList()));

        when(abakusArbeidsforholdTjeneste.hentArbeidsforholdInfoForEnPeriode(any(), any(), any(), any())).thenReturn(arbeidsforholdMedPermisjon);

        var morAktivitet = morsAktivitetInnhenter.finnMorsAktivitet(behandling, perioderMedAktivitetskrav, annenPartAktørId, Optional.empty())
            .orElseThrow();
        var aktivitetPeriodeEntitetListe = morAktivitet.perioderEntitet().getAktivitetskravArbeidPeriodeListe();


        assertThat(morAktivitet.fraDato()).isEqualTo(innhentingsperiodeFraDato.minusWeeks(2));
        assertThat(morAktivitet.tilDato()).isEqualTo(innhentingsperiodeTilDato.plusWeeks(2));
        assertThat(aktivitetPeriodeEntitetListe).hasSize(7);

        assertThat(
            aktivitetPeriodeEntitetListe.stream().filter(aktPeriode -> aktPeriode.getOrgNummer().getId().equals(orgnr1.getOrgnr())).toList()).hasSize(
            4);
        assertThat(aktivitetPeriodeEntitetListe.stream().filter(aktPeriode -> aktPeriode.getOrgNummer().getId().equals(orgnr1.getOrgnr()))).anyMatch(
            periode -> periode.getSumStillingsprosent().getVerdi().equals(BigDecimal.valueOf(55)));
        assertThat(aktivitetPeriodeEntitetListe.stream().filter(aktPeriode -> aktPeriode.getOrgNummer().getId().equals(orgnr1.getOrgnr()))).anyMatch(
            periode -> periode.getSumPermisjonsprosent().getVerdi().equals(BigDecimal.valueOf(20)) && periode.getPermisjonsbeskrivelseType()
                .equals(AktivitetskravPermisjonType.ANNEN_PERMISJON));
        assertThat(aktivitetPeriodeEntitetListe.stream().filter(aktPeriode -> aktPeriode.getOrgNummer().getId().equals(orgnr2.getOrgnr()))).anyMatch(
            periode -> periode.getSumStillingsprosent().getVerdi().equals(BigDecimal.valueOf(25)));
        assertThat(aktivitetPeriodeEntitetListe.stream()
            .filter(aktPeriode -> aktPeriode.getOrgNummer().getId().equals(orgnr2.getOrgnr()))
            .map(periode -> periode.getSumPermisjonsprosent().getVerdi())
            .reduce(BigDecimal::add)
            .orElse(BigDecimal.ZERO)).isEqualTo(BigDecimal.ZERO);
        assertThat(
            aktivitetPeriodeEntitetListe.stream().filter(aktPeriode -> aktPeriode.getOrgNummer().getId().equals(orgnr2.getOrgnr())).toList()).hasSize(
            3);
    }

    private ArbeidsforholdTjeneste.Permisjon lagPermisjon(DatoIntervallEntitet periode, BigDecimal permisjonsprosent) {
        return new ArbeidsforholdTjeneste.Permisjon(periode, PermisjonsbeskrivelseType.ANNEN_PERMISJON_LOVFESTET, permisjonsprosent);
    }

    private ArbeidsforholdMedPermisjon lagArbeidsforholdMedPermisjon(Arbeidsgiver arbeidsgiver,
                                                                     EksternArbeidsforholdRef ref,
                                                                     List<ArbeidsforholdTjeneste.AktivitetAvtale> aktivitetsavtaler,
                                                                     List<ArbeidsforholdTjeneste.Permisjon> permisjoner) {
        return new ArbeidsforholdMedPermisjon(arbeidsgiver, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, ref, aktivitetsavtaler, permisjoner);
    }

    private ArbeidsforholdTjeneste.AktivitetAvtale lagAktivitetsavtale(DatoIntervallEntitet periode, BigDecimal stillingsprosent) {
        return new ArbeidsforholdTjeneste.AktivitetAvtale(periode, stillingsprosent);
    }

    private OppgittPeriodeEntitet lagOppgittPeriode(LocalDate fraDato, LocalDate tildato) {
        return OppgittPeriodeBuilder.ny()
            .medPeriode(fraDato, tildato)
            .medPeriodeType(UttakPeriodeType.FELLESPERIODE)
            .medMorsAktivitet(MorsAktivitet.ARBEID)
            .build();
    }

    private AktivitetskravGrunnlagEntitet lagAktivitetsgrunnlag(Long behandlingId,
                                                                LocalDate fra,
                                                                LocalDate til,
                                                                AktivitetskravArbeidPerioderEntitet perioderEntitet) {
        return AktivitetskravGrunnlagEntitet.Builder.nytt()
            .medBehandlingId(behandlingId)
            .medPeriode(fra, til)
            .medPerioderMedAktivitetskravArbeid(perioderEntitet)
            .build();
    }

    private AktivitetskravArbeidPerioderEntitet lagPerioderBuilder(List<AktivitetskravArbeidPeriodeEntitet.Builder> perioder) {
        var builder = new AktivitetskravArbeidPerioderEntitet.Builder();
        perioder.forEach(builder::leggTil);
        return builder.build();
    }

    private AktivitetskravArbeidPeriodeEntitet.Builder lagAktvitetskravArbeidPeriode(LocalDate fra,
                                                                                     LocalDate til,
                                                                                     BigDecimal stillingsprosent,
                                                                                     String orgnr) {
        return new AktivitetskravArbeidPeriodeEntitet.Builder().medPeriode(fra, til)
            .medOrgNummer(orgnr)
            .medPermisjon(BigDecimal.ZERO, AktivitetskravPermisjonType.UDEFINERT)
            .medSumStillingsprosent(stillingsprosent);
    }

}
