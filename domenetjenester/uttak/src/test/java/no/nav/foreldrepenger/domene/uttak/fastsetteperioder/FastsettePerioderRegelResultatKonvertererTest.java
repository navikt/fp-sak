package no.nav.foreldrepenger.domene.uttak.fastsetteperioder;


import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering.Type.SYKDOM_SØKER_GODKJENT;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.DokumentasjonVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakUtsettelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.UttakBeregningsandelTjenesteTestUtil;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.FastsettePeriodeResultat;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Trekkdager;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AktivitetIdentifikator;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.OppgittPeriode;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Orgnummer;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Perioderesultattype;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.SamtidigUttaksprosent;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Stønadskontotype;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Utbetalingsgrad;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.UtsettelseÅrsak;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.UttakPeriode;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.UttakPeriodeAktivitet;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.utfall.InnvilgetÅrsak;

class FastsettePerioderRegelResultatKonvertererTest {

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryStubProvider();

    private final AbakusInMemoryInntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private final UttakBeregningsandelTjenesteTestUtil beregningsandelTjeneste = new UttakBeregningsandelTjenesteTestUtil();

    private final FastsettePerioderRegelResultatKonverterer konverterer = new FastsettePerioderRegelResultatKonverterer(
        repositoryProvider.getFpUttakRepository(), repositoryProvider.getYtelsesFordelingRepository());

    private UttakInput lagInput(Behandling behandling, LocalDate stp) {
        var ref = BehandlingReferanse.fra(behandling);
        return new UttakInput(ref, Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(stp).build(), iayTjeneste.hentGrunnlag(behandling.getId()),
            new ForeldrepengerGrunnlag()).medBeregningsgrunnlagStatuser(beregningsandelTjeneste.hentStatuser());
    }

    @Test
    void utsettelse_pga_arbeid_skal_sette_stillingsprosent_som_arbeidsprosent() {

        var periodeFom = LocalDate.of(2020, 1, 1);
        var periodeTom = LocalDate.of(2020, 2, 2);
        var oppgittPeriode = OppgittPeriodeBuilder.ny()
            .medPeriode(periodeFom, periodeTom)
            .medÅrsak(no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak.SYKDOM)
            .medPeriodeKilde(FordelingPeriodeKilde.SØKNAD)
            .medPeriodeType(UttakPeriodeType.UDEFINERT)
            .medDokumentasjonVurdering(new DokumentasjonVurdering(SYKDOM_SØKER_GODKJENT))
            .build();
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel()
            .medFordeling(new OppgittFordelingEntitet(List.of(oppgittPeriode), true));
        var behandling = scenario.lagre(repositoryProvider);
        var stillingsprosent = BigDecimal.valueOf(50);
        var arbeidsgiver = Arbeidsgiver.virksomhet("123");

        byggArbeidForBehandling(behandling, arbeidsgiver, stillingsprosent);
        beregningsandelTjeneste.leggTilOrdinærtArbeid(arbeidsgiver, InternArbeidsforholdRef.nullRef());

        var aktivitet = new UttakPeriodeAktivitet(
            AktivitetIdentifikator.forArbeid(new Orgnummer(arbeidsgiver.getIdentifikator()), null), Utbetalingsgrad.TEN,
            Trekkdager.ZERO, false);
        var uttakOppgittPeriode = OppgittPeriode.forUtsettelse(periodeFom, periodeTom,
            UtsettelseÅrsak.ARBEID, periodeFom, periodeFom, null, null);
        var uttakPeriode = new UttakPeriode(uttakOppgittPeriode, Perioderesultattype.INNVILGET, null,
            InnvilgetÅrsak.UTSETTELSE_GYLDIG_PGA_100_PROSENT_ARBEID, null, Set.of(aktivitet), SamtidigUttaksprosent.ZERO, Stønadskontotype.MØDREKVOTE);
        var fastsetteResultat = List.of(new FastsettePeriodeResultat(uttakPeriode, null, null, null));
        var input = lagInput(behandling, periodeFom);
        var konvertert = konverterer.konverter(input, fastsetteResultat, null);

        assertThat(konvertert.getPerioder()).hasSize(1);
        var utsettelse = konvertert.getPerioder().get(0);
        assertThat(utsettelse.getAktiviteter()).hasSize(1);
        assertThat(utsettelse.getAktiviteter().get(0).getArbeidsprosent()).isEqualTo(stillingsprosent);
        assertThat(utsettelse.getUtsettelseType()).isEqualTo(UttakUtsettelseType.ARBEID);
        assertThat(utsettelse.getPeriodeSøknad()).isPresent();
        assertThat(utsettelse.getPeriodeSøknad().get().getDokumentasjonVurdering()).isEqualTo(oppgittPeriode.getDokumentasjonVurdering());
    }

    private void byggArbeidForBehandling(Behandling behandling,
                                         Arbeidsgiver arbeidsgiver,
                                         BigDecimal stillingsprosent) {
        var inntektArbeidYtelseAggregatBuilder = iayTjeneste.opprettBuilderForRegister(behandling.getId());
        var aktørArbeidBuilder = inntektArbeidYtelseAggregatBuilder.getAktørArbeidBuilder(behandling.getAktørId());

        var yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(
            new Opptjeningsnøkkel(null, arbeidsgiver.getIdentifikator(), null), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);

        var aktivitetsAvtale = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.MIN, LocalDate.MAX))
            .medProsentsats(stillingsprosent)
            .medSisteLønnsendringsdato(LocalDate.now());

        var ansettelesperiode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.MIN, LocalDate.MAX));

        yrkesaktivitetBuilder.medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver)
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .leggTilAktivitetsAvtale(ansettelesperiode)
            .build();

        var aktørArbeid = aktørArbeidBuilder.leggTilYrkesaktivitet(yrkesaktivitetBuilder);
        inntektArbeidYtelseAggregatBuilder.leggTilAktørArbeid(aktørArbeid);

        iayTjeneste.lagreIayAggregat(behandling.getId(), inntektArbeidYtelseAggregatBuilder);
    }
}
