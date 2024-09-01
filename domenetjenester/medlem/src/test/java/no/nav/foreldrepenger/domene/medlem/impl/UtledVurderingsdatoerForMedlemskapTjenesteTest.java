package no.nav.foreldrepenger.domene.medlem.impl;

import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.aktør.OppholdstillatelseType;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapDekningType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapKildeType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.Personopplysning;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.Statsborgerskap;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.medlem.UtledVurderingsdatoerForMedlemskapTjeneste;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@CdiDbAwareTest
class UtledVurderingsdatoerForMedlemskapTjenesteTest {

    @Inject
    private BehandlingRepositoryProvider provider;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private MedlemskapRepository medlemskapRepository;
    @Inject
    private PersonopplysningRepository personopplysningRepository;
    @Inject
    private FagsakRepository fagsakRepository;
    @Inject
    private UtledVurderingsdatoerForMedlemskapTjeneste tjeneste;
    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;

    @Test
    void skal_ikke_utlede_dato_når_overlappende_perioder_uten_endring_i_medl() {
        // Arrange
        var startdato = LocalDate.now();
        var sluttdato = startdato.plusWeeks(53);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(startdato).build());
        scenario.medDefaultSøknadTerminbekreftelse();
        var periode = new MedlemskapPerioderBuilder()
                .medDekningType(MedlemskapDekningType.FULL)
                .medMedlemskapType(MedlemskapType.ENDELIG)
                .medPeriode(LocalDate.of(2014, 2, 17), TIDENES_ENDE)
                .medKildeType(MedlemskapKildeType.AVGSYS)
                .medBeslutningsdato(LocalDate.of(2014, 3, 5))
                .medMedlId(1L)
                .build();
        var periode2 = new MedlemskapPerioderBuilder()
                .medDekningType(MedlemskapDekningType.FULL)
                .medMedlemskapType(MedlemskapType.ENDELIG)
                .medPeriode(LocalDate.of(2017, 1, 1), TIDENES_ENDE)
                .medKildeType(MedlemskapKildeType.AVGSYS)
                .medBeslutningsdato(LocalDate.of(2018, 8, 31))
                .medMedlId(1L)
                .build();

        scenario.leggTilMedlemskapPeriode(periode);
        scenario.leggTilMedlemskapPeriode(periode2);
        var behandling = scenario.lagre(provider);
        avslutterBehandlingOgFagsak(behandling);

        var revudering = opprettRevudering(behandling);

        // Act
        var vurderingsdatoer = tjeneste.finnVurderingsdatoer(lagRef(revudering), lagStp(startdato, sluttdato));

        // Assert
        assertThat(vurderingsdatoer).isEmpty();
    }

    @Test
    void skal_utlede_dato_når_overlappende_perioder_med_endring_i_periode_med_senest_beslutningsdato() {
        // Arrange
        var startdato = LocalDate.now();
        var sluttdato = startdato.plusWeeks(53);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(startdato).build());
        scenario.medDefaultSøknadTerminbekreftelse();
        var periode = new MedlemskapPerioderBuilder()
                .medDekningType(MedlemskapDekningType.FULL)
                .medMedlemskapType(MedlemskapType.ENDELIG)
                .medPeriode(LocalDate.of(2014, 2, 17), TIDENES_ENDE)
                .medKildeType(MedlemskapKildeType.AVGSYS)
                .medBeslutningsdato(LocalDate.of(2014, 3, 5))
                .medMedlId(1L)
                .build();
        var periode2 = new MedlemskapPerioderBuilder()
                .medDekningType(MedlemskapDekningType.FULL)
                .medMedlemskapType(MedlemskapType.ENDELIG)
                .medPeriode(LocalDate.of(2017, 1, 1), TIDENES_ENDE)
                .medKildeType(MedlemskapKildeType.AVGSYS)
                .medBeslutningsdato(LocalDate.of(2018, 8, 31))
                .medMedlId(1L)
                .build();

        scenario.leggTilMedlemskapPeriode(periode);
        scenario.leggTilMedlemskapPeriode(periode2);
        var behandling = scenario.lagre(provider);
        avslutterBehandlingOgFagsak(behandling);

        var revudering = opprettRevudering(behandling);
        var revurderingId = revudering.getId();

        var endringsdato = startdato.plusMonths(1);
        var endretPeriode = new MedlemskapPerioderBuilder()
                .medDekningType(MedlemskapDekningType.FULL)
                .medMedlemskapType(MedlemskapType.ENDELIG)
                .medPeriode(endringsdato, TIDENES_ENDE)
                .medKildeType(MedlemskapKildeType.AVGSYS)
                .medBeslutningsdato(LocalDate.of(2018, 8, 31))
                .medMedlId(1L)
                .build();
        medlemskapRepository.lagreMedlemskapRegisterOpplysninger(revurderingId, List.of(periode, endretPeriode));

        // Act
        var vurderingsdatoer = tjeneste.finnVurderingsdatoer(lagRef(revudering), lagStp(startdato, sluttdato));

        // Assert
        assertThat(vurderingsdatoer).containsExactly(endringsdato);
    }

    @Test
    void skal_utled_vurderingsdato_ved_endring_i_medlemskapsperioder() {
        // Arrange
        var startdato = LocalDate.now();
        var sluttdato = startdato.plusWeeks(53);
        var datoMedEndring = startdato.plusDays(10);
        var ettÅrSiden = startdato.minusYears(1);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(startdato).build());
        scenario.medDefaultSøknadTerminbekreftelse();
        var periode = opprettPeriode(ettÅrSiden, startdato, MedlemskapDekningType.FTL_2_6);
        scenario.leggTilMedlemskapPeriode(periode);
        var behandling = scenario.lagre(provider);
        avslutterBehandlingOgFagsak(behandling);

        var revudering = opprettRevudering(behandling);
        var revurderingId = revudering.getId();

        oppdaterMedlem(datoMedEndring, periode, revurderingId);

        // Act
        var vurderingsdatoer = tjeneste.finnVurderingsdatoer(lagRef(revudering), lagStp(startdato, sluttdato));

        // Assert
        assertThat(vurderingsdatoer).containsExactly(datoMedEndring);
    }

    @Test
    void skal_utled_vurderingsdato_ved_endring_personopplysninger_statsborgerskap() {
        // Arrange
        var startdato = LocalDate.now();
        var sluttdato = startdato.plusYears(3);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medDefaultSøknadTerminbekreftelse();
        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(startdato).build());
        var aktørId = scenario.getDefaultBrukerAktørId();

        var personopplysningBuilder = scenario.opprettBuilderForRegisteropplysninger();
        personopplysningBuilder.leggTilPersonopplysninger(
            Personopplysning.builder().aktørId(aktørId).sivilstand(SivilstandType.GIFT)
                .fødselsdato(startdato).brukerKjønn(NavBrukerKjønn.KVINNE).navn("Marie Curie"))
            .leggTilStatsborgerskap(
                Statsborgerskap.builder().aktørId(aktørId).periode(startdato, startdato.plusYears(1)).statsborgerskap(Landkoder.ARG))
            .leggTilStatsborgerskap(
                Statsborgerskap.builder().aktørId(aktørId).periode(startdato.plusYears(1), startdato.plusYears(2)).statsborgerskap(Landkoder.ESP))
            .leggTilStatsborgerskap(
                Statsborgerskap.builder().aktørId(aktørId).periode(startdato.plusYears(2), startdato.plusYears(3).minusWeeks(1)).statsborgerskap(Landkoder.NOR));

        scenario.medRegisterOpplysninger(personopplysningBuilder.build());

        var behandling = scenario.lagre(provider);
        var behandlingId = behandling.getId();

        // Act
        var vurderingsdatoer = tjeneste.finnVurderingsdatoer(lagRef(behandling), lagStp(startdato, sluttdato));

        assertThat(vurderingsdatoer).containsExactlyInAnyOrder(startdato.plusYears(1), startdato.plusYears(2));
    }

    @Test
    void skal_utled_vurderingsdato_ved_endring_personopplysninger_personstatus_skal_ikke_se_på_død() {
        // Arrange
        var startdato = LocalDate.now();
        var sluttdato = startdato.plusYears(3);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medDefaultSøknadTerminbekreftelse();
        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now()).build());
        var søkerAktørId = scenario.getDefaultBrukerAktørId();

        var førsteÅr = DatoIntervallEntitet.fraOgMedTilOgMed(startdato, startdato.plusYears(1));
        var andreÅr = DatoIntervallEntitet.fraOgMedTilOgMed(startdato.plusYears(1), startdato.plusYears(2));
        var tredjeÅr = DatoIntervallEntitet.fraOgMedTilOgMed(startdato.plusYears(2), startdato.plusYears(3));
        var behandling = scenario.lagre(provider);
        var behandlingId = behandling.getId();
        var personopplysningGrunnlag = personopplysningRepository.hentPersonopplysninger(behandlingId);

        var personInformasjonBuilder = PersonInformasjonBuilder.oppdater(personopplysningGrunnlag.getRegisterVersjon(),
                PersonopplysningVersjonType.REGISTRERT);
        var førsteÅrBosa = personInformasjonBuilder.getPersonstatusBuilder(søkerAktørId, førsteÅr)
                .medPersonstatus(PersonstatusType.BOSA);
        var andreÅrBosa = personInformasjonBuilder.getPersonstatusBuilder(søkerAktørId, andreÅr)
                .medPersonstatus(PersonstatusType.UTVA);
        var tredjeÅrBosa = personInformasjonBuilder.getPersonstatusBuilder(søkerAktørId, tredjeÅr)
                .medPersonstatus(PersonstatusType.DØD);
        personInformasjonBuilder.leggTil(førsteÅrBosa);
        personInformasjonBuilder.leggTil(andreÅrBosa);
        personInformasjonBuilder.leggTil(tredjeÅrBosa);

        personopplysningRepository.lagre(behandlingId, personInformasjonBuilder);

        // Act
        var vurderingsdatoer = tjeneste.finnVurderingsdatoer(lagRef(behandling), lagStp(startdato, sluttdato));

        assertThat(vurderingsdatoer).containsExactlyInAnyOrder(andreÅr.getFomDato());
    }

    @Test
    void skal_utled_vurderingsdato_ved_endring_personopplysninger_adressetype() {
        // Arrange
        var startdato = LocalDate.now();
        var sluttdato = startdato.plusYears(3);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medDefaultSøknadTerminbekreftelse();
        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(startdato).build());
        var søkerAktørId = scenario.getDefaultBrukerAktørId();

        var førsteÅr = DatoIntervallEntitet.fraOgMedTilOgMed(startdato, startdato.plusYears(1));
        var andreÅr = DatoIntervallEntitet.fraOgMedTilOgMed(startdato.plusYears(1), startdato.plusYears(2));
        var tredjeÅr = DatoIntervallEntitet.fraOgMedTilOgMed(startdato.plusYears(2), startdato.plusYears(3));
        var behandling = scenario.lagre(provider);
        var behandlingId = behandling.getId();
        var personopplysningGrunnlag = personopplysningRepository.hentPersonopplysninger(behandlingId);

        var personInformasjonBuilder = PersonInformasjonBuilder.oppdater(personopplysningGrunnlag.getRegisterVersjon(),
                PersonopplysningVersjonType.REGISTRERT);
        var bostedFørsteÅr = personInformasjonBuilder.getAdresseBuilder(søkerAktørId, førsteÅr,
                AdresseType.BOSTEDSADRESSE).medLand(Landkoder.XUK.getKode());
        var utlandAndreÅr = personInformasjonBuilder.getAdresseBuilder(søkerAktørId, andreÅr,
                AdresseType.BOSTEDSADRESSE).medLand(Landkoder.NOR.getKode());
        var bostedTredjeÅr = personInformasjonBuilder.getAdresseBuilder(søkerAktørId, tredjeÅr,
                AdresseType.POSTADRESSE_UTLAND);
        personInformasjonBuilder.leggTil(bostedFørsteÅr);
        personInformasjonBuilder.leggTil(utlandAndreÅr);
        personInformasjonBuilder.leggTil(bostedTredjeÅr);

        personopplysningRepository.lagre(behandlingId, personInformasjonBuilder);

        // Act
        var vurderingsdatoer = tjeneste.finnVurderingsdatoer(lagRef(behandling), lagStp(startdato, sluttdato));

        assertThat(vurderingsdatoer).containsExactlyInAnyOrder(førsteÅr.getTomDato().plusDays(1), tredjeÅr.getFomDato());
    }

    @Test
    void skal_utlede_vurderingsdato_ved_opphold_tom_i_uttaket() {
        // Arrange
        var startdato = LocalDate.now();
        var sluttdato = startdato.plusWeeks(53);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medDefaultSøknadTerminbekreftelse();
        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(startdato).build());
        var søkerAktørId = scenario.getDefaultBrukerAktørId();

        var behandling = scenario.lagre(provider);
        var behandlingId = behandling.getId();
        var personopplysningGrunnlag = personopplysningRepository.hentPersonopplysninger(behandlingId);

        var personInformasjonBuilder = PersonInformasjonBuilder.oppdater(personopplysningGrunnlag.getRegisterVersjon(),
            PersonopplysningVersjonType.REGISTRERT);
        var midlertidig = personInformasjonBuilder
            .getOppholdstillatelseBuilder(søkerAktørId, DatoIntervallEntitet.fraOgMedTilOgMed(startdato.minusMonths(15), startdato.plusMonths(9)))
            .medOppholdstillatelse(OppholdstillatelseType.MIDLERTIDIG);
        personInformasjonBuilder.leggTil(midlertidig);

        personopplysningRepository.lagre(behandlingId, personInformasjonBuilder);

        // Act
        var vurderingsdatoer = tjeneste.finnVurderingsdatoer(lagRef(behandling), lagStp(startdato, sluttdato));

        assertThat(vurderingsdatoer).containsExactlyInAnyOrder(startdato.plusMonths(9).plusDays(1));
    }

    @Test
    void skal_ikke_utlede_vurderingsdato_som_ligger_før_skjæringstidspunkt() {
        // Arrange
        var startdato = LocalDate.now();
        var sluttdato = startdato.plusWeeks(51);
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medDefaultSøknadTerminbekreftelse();
        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(startdato).build());
        var søkerAktørId = scenario.getDefaultBrukerAktørId();

        var førsteÅr = DatoIntervallEntitet.fraOgMedTilOgMed(startdato.minusYears(3), startdato.minusYears(2));
        var andreÅr = DatoIntervallEntitet.fraOgMedTilOgMed(startdato.minusYears(2), startdato.minusYears(1));
        var tredjeÅr = DatoIntervallEntitet.fraOgMedTilOgMed(startdato, startdato.plusYears(1));
        var behandling = scenario.lagre(provider);
        var behandlingId = behandling.getId();
        var personopplysningGrunnlag = personopplysningRepository.hentPersonopplysninger(behandlingId);

        var personInformasjonBuilder = PersonInformasjonBuilder.oppdater(personopplysningGrunnlag.getRegisterVersjon(),
                PersonopplysningVersjonType.REGISTRERT);
        var bostedFørsteÅr = personInformasjonBuilder.getAdresseBuilder(søkerAktørId, førsteÅr,
                AdresseType.BOSTEDSADRESSE);
        var utlandAndreÅr = personInformasjonBuilder.getAdresseBuilder(søkerAktørId, andreÅr,
                AdresseType.POSTADRESSE_UTLAND);
        var bostedTredjeÅr = personInformasjonBuilder.getAdresseBuilder(søkerAktørId, tredjeÅr,
                AdresseType.BOSTEDSADRESSE);
        personInformasjonBuilder.leggTil(bostedFørsteÅr);
        personInformasjonBuilder.leggTil(utlandAndreÅr);
        personInformasjonBuilder.leggTil(bostedTredjeÅr);

        personopplysningRepository.lagre(behandlingId, personInformasjonBuilder);

        // Act
        var vurderingsdatoer = tjeneste.finnVurderingsdatoer(lagRef(behandling), lagStp(startdato, sluttdato));

        assertThat(vurderingsdatoer).isEmpty();
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        return BehandlingReferanse.fra(behandling);
    }

    private Skjæringstidspunkt lagStp(LocalDate fom, LocalDate tom) {
        return Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(fom)
            .medUttaksintervall(new LocalDateInterval(fom, tom))
            .medFørsteUttaksdato(fom)
            .medSkjæringstidspunktOpptjening(fom)
            .build();
    }

    private void oppdaterMedlem(LocalDate datoMedEndring, MedlemskapPerioderEntitet periode, Long behandlingId) {
        var nyPeriode = new MedlemskapPerioderBuilder()
                .medPeriode(datoMedEndring, null)
                .medDekningType(MedlemskapDekningType.FULL)
                .medMedlemskapType(MedlemskapType.ENDELIG)
                .medKildeType(MedlemskapKildeType.MEDL)
                .medMedlId(2L)
                .build();
        medlemskapRepository.lagreMedlemskapRegisterOpplysninger(behandlingId, List.of(periode, nyPeriode));
    }

    private Behandling opprettRevudering(Behandling behandling) {
        var revurderingÅrsak = BehandlingÅrsak.builder(BehandlingÅrsakType.RE_FEIL_ELLER_ENDRET_FAKTA)
                .medOriginalBehandlingId(behandling.getId());

        var revudering = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
                .medBehandlingÅrsak(revurderingÅrsak).build();

        var behandlingId = behandling.getId();
        // TODO(FC): Her burde kanskje behandlingId vært låst inntil revurdering er
        // opprettet?
        behandlingRepository.lagre(revudering, behandlingRepository.taSkriveLås(revudering.getId()));
        var revurderingId = revudering.getId();
        medlemskapRepository.kopierGrunnlagFraEksisterendeBehandling(behandlingId, revurderingId);
        iayTjeneste.kopierGrunnlagFraEksisterendeBehandling(behandlingId, revurderingId);
        personopplysningRepository.kopierGrunnlagFraEksisterendeBehandlingUtenVurderinger(behandlingId, revurderingId);

        return revudering;
    }

    private void avslutterBehandlingOgFagsak(Behandling behandling) {
        var lås = behandlingRepository.taSkriveLås(behandling);
        provider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(behandling.getId(), lagUttaksPeriode());

        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, lås);
        fagsakRepository.oppdaterFagsakStatus(behandling.getFagsakId(), FagsakStatus.AVSLUTTET);
    }

    private MedlemskapPerioderEntitet opprettPeriode(LocalDate fom, LocalDate tom, MedlemskapDekningType dekningType) {
        return new MedlemskapPerioderBuilder()
                .medDekningType(dekningType)
                .medMedlemskapType(MedlemskapType.FORELOPIG)
                .medPeriode(fom, tom)
                .medKildeType(MedlemskapKildeType.MEDL)
                .medMedlId(1L)
                .build();
    }

    private UttakResultatPerioderEntitet lagUttaksPeriode() {
        var idag = LocalDate.now();
        var periode = new UttakResultatPeriodeEntitet.Builder(idag, idag.plusDays(6))
                .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
                .build();
        var uttakAktivtet = new UttakAktivitetEntitet.Builder()
                .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
                .medArbeidsforhold(Arbeidsgiver.virksomhet("123"), InternArbeidsforholdRef.nyRef())
                .build();
        var periodeAktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(periode, uttakAktivtet)
                .medUtbetalingsgrad(new Utbetalingsgrad(100))
                .medArbeidsprosent(BigDecimal.valueOf(100L))
                .medErSøktGradering(true)
                .medTrekkonto(UttakPeriodeType.MØDREKVOTE)
                .build();
        periode.leggTilAktivitet(periodeAktivitet);
        var perioder = new UttakResultatPerioderEntitet();
        perioder.leggTilPeriode(periode);
        return perioder;
    }

}
