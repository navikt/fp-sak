package no.nav.foreldrepenger.domene.medlem.impl;

import static no.nav.vedtak.konfig.Tid.TIDENES_ENDE;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
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
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.AvklarteUttakDatoerEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.medlem.UtledVurderingsdatoerForMedlemskapTjeneste;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.fpsak.tidsserie.LocalDateInterval;

@CdiDbAwareTest
public class UtledVurderingsdatoerForMedlemskapTjenesteTest {

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
    public void skal_ikke_utlede_dato_når_overlappende_perioder_uten_endring_i_medl() {
        // Arrange
        var startdato = LocalDate.now();
        var sluttdato = startdato.plusWeeks(53);
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(startdato).build());
        scenario.medDefaultSøknadTerminbekreftelse();
        MedlemskapPerioderEntitet periode = new MedlemskapPerioderBuilder()
                .medDekningType(MedlemskapDekningType.FULL)
                .medMedlemskapType(MedlemskapType.ENDELIG)
                .medPeriode(LocalDate.of(2014, 2, 17), TIDENES_ENDE)
                .medKildeType(MedlemskapKildeType.AVGSYS)
                .medBeslutningsdato(LocalDate.of(2014, 3, 5))
                .medMedlId(1L)
                .build();
        MedlemskapPerioderEntitet periode2 = new MedlemskapPerioderBuilder()
                .medDekningType(MedlemskapDekningType.FULL)
                .medMedlemskapType(MedlemskapType.ENDELIG)
                .medPeriode(LocalDate.of(2017, 1, 1), TIDENES_ENDE)
                .medKildeType(MedlemskapKildeType.AVGSYS)
                .medBeslutningsdato(LocalDate.of(2018, 8, 31))
                .medMedlId(1L)
                .build();

        scenario.leggTilMedlemskapPeriode(periode);
        scenario.leggTilMedlemskapPeriode(periode2);
        Behandling behandling = scenario.lagre(provider);
        avslutterBehandlingOgFagsak(behandling);

        Behandling revudering = opprettRevudering(behandling);

        // Act
        Set<LocalDate> vurderingsdatoer = tjeneste.finnVurderingsdatoer(lagRef(revudering, startdato, sluttdato));

        // Assert
        assertThat(vurderingsdatoer).isEmpty();
    }

    @Test
    public void skal_utlede_dato_når_overlappende_perioder_med_endring_i_periode_med_senest_beslutningsdato() {
        // Arrange
        var startdato = LocalDate.now();
        var sluttdato = startdato.plusWeeks(53);
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(startdato).build());
        scenario.medDefaultSøknadTerminbekreftelse();
        MedlemskapPerioderEntitet periode = new MedlemskapPerioderBuilder()
                .medDekningType(MedlemskapDekningType.FULL)
                .medMedlemskapType(MedlemskapType.ENDELIG)
                .medPeriode(LocalDate.of(2014, 2, 17), TIDENES_ENDE)
                .medKildeType(MedlemskapKildeType.AVGSYS)
                .medBeslutningsdato(LocalDate.of(2014, 3, 5))
                .medMedlId(1L)
                .build();
        MedlemskapPerioderEntitet periode2 = new MedlemskapPerioderBuilder()
                .medDekningType(MedlemskapDekningType.FULL)
                .medMedlemskapType(MedlemskapType.ENDELIG)
                .medPeriode(LocalDate.of(2017, 1, 1), TIDENES_ENDE)
                .medKildeType(MedlemskapKildeType.AVGSYS)
                .medBeslutningsdato(LocalDate.of(2018, 8, 31))
                .medMedlId(1L)
                .build();

        scenario.leggTilMedlemskapPeriode(periode);
        scenario.leggTilMedlemskapPeriode(periode2);
        Behandling behandling = scenario.lagre(provider);
        avslutterBehandlingOgFagsak(behandling);

        Behandling revudering = opprettRevudering(behandling);
        Long revurderingId = revudering.getId();

        LocalDate endringsdato = startdato.plusMonths(1);
        MedlemskapPerioderEntitet endretPeriode = new MedlemskapPerioderBuilder()
                .medDekningType(MedlemskapDekningType.FULL)
                .medMedlemskapType(MedlemskapType.ENDELIG)
                .medPeriode(endringsdato, TIDENES_ENDE)
                .medKildeType(MedlemskapKildeType.AVGSYS)
                .medBeslutningsdato(LocalDate.of(2018, 8, 31))
                .medMedlId(1L)
                .build();
        medlemskapRepository.lagreMedlemskapRegisterOpplysninger(revurderingId, List.of(periode, endretPeriode));

        // Act
        Set<LocalDate> vurderingsdatoer = tjeneste.finnVurderingsdatoer(lagRef(revudering, startdato, sluttdato));

        // Assert
        assertThat(vurderingsdatoer).containsExactly(endringsdato);
    }

    @Test
    public void skal_utled_vurderingsdato_ved_endring_i_medlemskapsperioder() {
        // Arrange
        var startdato = LocalDate.now();
        var sluttdato = startdato.plusWeeks(53);
        LocalDate datoMedEndring = startdato.plusDays(10);
        LocalDate ettÅrSiden = startdato.minusYears(1);
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(startdato).build());
        scenario.medDefaultSøknadTerminbekreftelse();
        MedlemskapPerioderEntitet periode = opprettPeriode(ettÅrSiden, startdato, MedlemskapDekningType.FTL_2_6);
        scenario.leggTilMedlemskapPeriode(periode);
        Behandling behandling = scenario.lagre(provider);
        avslutterBehandlingOgFagsak(behandling);

        Behandling revudering = opprettRevudering(behandling);
        Long revurderingId = revudering.getId();

        oppdaterMedlem(datoMedEndring, periode, revurderingId);

        // Act
        Set<LocalDate> vurderingsdatoer = tjeneste.finnVurderingsdatoer(lagRef(revudering, startdato, sluttdato));

        // Assert
        assertThat(vurderingsdatoer).containsExactly(datoMedEndring);
    }

    @Test
    public void skal_utled_vurderingsdato_ved_endring_personopplysninger_statsborgerskap() {
        // Arrange
        var startdato = LocalDate.now();
        var sluttdato = startdato.plusYears(3);
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medDefaultSøknadTerminbekreftelse();
        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(startdato).build());
        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();

        DatoIntervallEntitet førsteÅr = DatoIntervallEntitet.fraOgMedTilOgMed(startdato, startdato.plusYears(1));
        DatoIntervallEntitet andreÅr = DatoIntervallEntitet.fraOgMedTilOgMed(startdato.plusYears(1), startdato.plusYears(2));
        DatoIntervallEntitet tredjeÅr = DatoIntervallEntitet.fraOgMedTilOgMed(startdato.plusYears(2), startdato.plusYears(3).minusWeeks(1));
        Behandling behandling = scenario.lagre(provider);
        Long behandlingId = behandling.getId();
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag = personopplysningRepository.hentPersonopplysninger(behandlingId);

        PersonInformasjonBuilder personInformasjonBuilder = PersonInformasjonBuilder.oppdater(personopplysningGrunnlag.getRegisterVersjon(),
                PersonopplysningVersjonType.REGISTRERT);
        PersonInformasjonBuilder.StatsborgerskapBuilder amerikaFørsteÅr = personInformasjonBuilder.getStatsborgerskapBuilder(søkerAktørId, førsteÅr,
                Landkoder.ARG, Region.TREDJELANDS_BORGER);
        PersonInformasjonBuilder.StatsborgerskapBuilder spaniaAndreÅr = personInformasjonBuilder.getStatsborgerskapBuilder(søkerAktørId, andreÅr,
                Landkoder.ESP, Region.EOS);
        PersonInformasjonBuilder.StatsborgerskapBuilder norgeTredjeÅr = personInformasjonBuilder.getStatsborgerskapBuilder(søkerAktørId, tredjeÅr,
                Landkoder.NOR, Region.NORDEN);
        personInformasjonBuilder.leggTil(amerikaFørsteÅr);
        personInformasjonBuilder.leggTil(spaniaAndreÅr);
        personInformasjonBuilder.leggTil(norgeTredjeÅr);

        personopplysningRepository.lagre(behandlingId, personInformasjonBuilder);

        // Act
        Set<LocalDate> vurderingsdatoer = tjeneste.finnVurderingsdatoer(lagRef(behandling, startdato, sluttdato));

        assertThat(vurderingsdatoer).containsExactlyInAnyOrder(andreÅr.getFomDato(), tredjeÅr.getFomDato());
    }

    @Test
    public void skal_utled_vurderingsdato_ved_endring_personopplysninger_personstatus_skal_ikke_se_på_død() {
        // Arrange
        var startdato = LocalDate.now();
        var sluttdato = startdato.plusYears(3);
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medDefaultSøknadTerminbekreftelse();
        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(LocalDate.now()).build());
        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();

        DatoIntervallEntitet førsteÅr = DatoIntervallEntitet.fraOgMedTilOgMed(startdato, startdato.plusYears(1));
        DatoIntervallEntitet andreÅr = DatoIntervallEntitet.fraOgMedTilOgMed(startdato.plusYears(1), startdato.plusYears(2));
        DatoIntervallEntitet tredjeÅr = DatoIntervallEntitet.fraOgMedTilOgMed(startdato.plusYears(2), startdato.plusYears(3));
        Behandling behandling = scenario.lagre(provider);
        Long behandlingId = behandling.getId();
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag = personopplysningRepository.hentPersonopplysninger(behandlingId);

        PersonInformasjonBuilder personInformasjonBuilder = PersonInformasjonBuilder.oppdater(personopplysningGrunnlag.getRegisterVersjon(),
                PersonopplysningVersjonType.REGISTRERT);
        PersonInformasjonBuilder.PersonstatusBuilder førsteÅrBosa = personInformasjonBuilder.getPersonstatusBuilder(søkerAktørId, førsteÅr)
                .medPersonstatus(PersonstatusType.BOSA);
        PersonInformasjonBuilder.PersonstatusBuilder andreÅrBosa = personInformasjonBuilder.getPersonstatusBuilder(søkerAktørId, andreÅr)
                .medPersonstatus(PersonstatusType.UTVA);
        PersonInformasjonBuilder.PersonstatusBuilder tredjeÅrBosa = personInformasjonBuilder.getPersonstatusBuilder(søkerAktørId, tredjeÅr)
                .medPersonstatus(PersonstatusType.DØD);
        personInformasjonBuilder.leggTil(førsteÅrBosa);
        personInformasjonBuilder.leggTil(andreÅrBosa);
        personInformasjonBuilder.leggTil(tredjeÅrBosa);

        personopplysningRepository.lagre(behandlingId, personInformasjonBuilder);

        // Act
        Set<LocalDate> vurderingsdatoer = tjeneste.finnVurderingsdatoer(lagRef(behandling, startdato, sluttdato));

        assertThat(vurderingsdatoer).containsExactlyInAnyOrder(andreÅr.getFomDato());
    }

    @Test
    public void skal_utled_vurderingsdato_ved_endring_personopplysninger_adressetype() {
        // Arrange
        var startdato = LocalDate.now();
        var sluttdato = startdato.plusYears(3);
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medDefaultSøknadTerminbekreftelse();
        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(startdato).build());
        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();

        DatoIntervallEntitet førsteÅr = DatoIntervallEntitet.fraOgMedTilOgMed(startdato, startdato.plusYears(1));
        DatoIntervallEntitet andreÅr = DatoIntervallEntitet.fraOgMedTilOgMed(startdato.plusYears(1), startdato.plusYears(2));
        DatoIntervallEntitet tredjeÅr = DatoIntervallEntitet.fraOgMedTilOgMed(startdato.plusYears(2), startdato.plusYears(3));
        Behandling behandling = scenario.lagre(provider);
        Long behandlingId = behandling.getId();
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag = personopplysningRepository.hentPersonopplysninger(behandlingId);

        PersonInformasjonBuilder personInformasjonBuilder = PersonInformasjonBuilder.oppdater(personopplysningGrunnlag.getRegisterVersjon(),
                PersonopplysningVersjonType.REGISTRERT);
        PersonInformasjonBuilder.AdresseBuilder bostedFørsteÅr = personInformasjonBuilder.getAdresseBuilder(søkerAktørId, førsteÅr,
                AdresseType.BOSTEDSADRESSE).medLand(Landkoder.XUK.getKode());
        PersonInformasjonBuilder.AdresseBuilder utlandAndreÅr = personInformasjonBuilder.getAdresseBuilder(søkerAktørId, andreÅr,
                AdresseType.BOSTEDSADRESSE).medLand(Landkoder.NOR.getKode());
        PersonInformasjonBuilder.AdresseBuilder bostedTredjeÅr = personInformasjonBuilder.getAdresseBuilder(søkerAktørId, tredjeÅr,
                AdresseType.POSTADRESSE_UTLAND);
        personInformasjonBuilder.leggTil(bostedFørsteÅr);
        personInformasjonBuilder.leggTil(utlandAndreÅr);
        personInformasjonBuilder.leggTil(bostedTredjeÅr);

        personopplysningRepository.lagre(behandlingId, personInformasjonBuilder);

        // Act
        Set<LocalDate> vurderingsdatoer = tjeneste.finnVurderingsdatoer(lagRef(behandling, startdato, sluttdato));

        assertThat(vurderingsdatoer).containsExactlyInAnyOrder(førsteÅr.getTomDato().plusDays(1), tredjeÅr.getFomDato());
    }

    @Test
    public void skal_utlede_vurderingsdato_ved_opphold_tom_i_uttaket() {
        // Arrange
        var startdato = LocalDate.now();
        var sluttdato = startdato.plusWeeks(53);
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medDefaultSøknadTerminbekreftelse();
        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(startdato).build());
        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();

        Behandling behandling = scenario.lagre(provider);
        Long behandlingId = behandling.getId();
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag = personopplysningRepository.hentPersonopplysninger(behandlingId);

        PersonInformasjonBuilder personInformasjonBuilder = PersonInformasjonBuilder.oppdater(personopplysningGrunnlag.getRegisterVersjon(),
            PersonopplysningVersjonType.REGISTRERT);
        PersonInformasjonBuilder.OppholdstillatelseBuilder midlertidig = personInformasjonBuilder
            .getOppholdstillatelseBuilder(søkerAktørId, DatoIntervallEntitet.fraOgMedTilOgMed(startdato.minusMonths(15), startdato.plusMonths(9)))
            .medOppholdstillatelse(OppholdstillatelseType.MIDLERTIDIG);
        personInformasjonBuilder.leggTil(midlertidig);

        personopplysningRepository.lagre(behandlingId, personInformasjonBuilder);

        // Act
        Set<LocalDate> vurderingsdatoer = tjeneste.finnVurderingsdatoer(lagRef(behandling, startdato, sluttdato));

        assertThat(vurderingsdatoer).containsExactlyInAnyOrder(startdato.plusMonths(9).plusDays(1));
    }

    @Test
    public void skal_ikke_utlede_vurderingsdato_som_ligger_før_skjæringstidspunkt() {
        // Arrange
        var startdato = LocalDate.now();
        var sluttdato = startdato.plusWeeks(51);
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        scenario.medDefaultSøknadTerminbekreftelse();
        scenario.medAvklarteUttakDatoer(new AvklarteUttakDatoerEntitet.Builder().medFørsteUttaksdato(startdato).build());
        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();

        DatoIntervallEntitet førsteÅr = DatoIntervallEntitet.fraOgMedTilOgMed(startdato.minusYears(3), startdato.minusYears(2));
        DatoIntervallEntitet andreÅr = DatoIntervallEntitet.fraOgMedTilOgMed(startdato.minusYears(2), startdato.minusYears(1));
        DatoIntervallEntitet tredjeÅr = DatoIntervallEntitet.fraOgMedTilOgMed(startdato, startdato.plusYears(1));
        Behandling behandling = scenario.lagre(provider);
        Long behandlingId = behandling.getId();
        PersonopplysningGrunnlagEntitet personopplysningGrunnlag = personopplysningRepository.hentPersonopplysninger(behandlingId);

        PersonInformasjonBuilder personInformasjonBuilder = PersonInformasjonBuilder.oppdater(personopplysningGrunnlag.getRegisterVersjon(),
                PersonopplysningVersjonType.REGISTRERT);
        PersonInformasjonBuilder.AdresseBuilder bostedFørsteÅr = personInformasjonBuilder.getAdresseBuilder(søkerAktørId, førsteÅr,
                AdresseType.BOSTEDSADRESSE);
        PersonInformasjonBuilder.AdresseBuilder utlandAndreÅr = personInformasjonBuilder.getAdresseBuilder(søkerAktørId, andreÅr,
                AdresseType.POSTADRESSE_UTLAND);
        PersonInformasjonBuilder.AdresseBuilder bostedTredjeÅr = personInformasjonBuilder.getAdresseBuilder(søkerAktørId, tredjeÅr,
                AdresseType.BOSTEDSADRESSE);
        personInformasjonBuilder.leggTil(bostedFørsteÅr);
        personInformasjonBuilder.leggTil(utlandAndreÅr);
        personInformasjonBuilder.leggTil(bostedTredjeÅr);

        personopplysningRepository.lagre(behandlingId, personInformasjonBuilder);

        // Act
        Set<LocalDate> vurderingsdatoer = tjeneste.finnVurderingsdatoer(lagRef(behandling, startdato, sluttdato));

        assertThat(vurderingsdatoer).isEmpty();
    }

    private BehandlingReferanse lagRef(Behandling behandling, LocalDate fom, LocalDate tom) {
        return BehandlingReferanse.fra(behandling, Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(fom)
            .medUtledetMedlemsintervall(new LocalDateInterval(fom, tom))
            .medFørsteUttaksdato(fom)
            .medSkjæringstidspunktOpptjening(fom)
            .build());
    }

    private void oppdaterMedlem(LocalDate datoMedEndring, MedlemskapPerioderEntitet periode, Long behandlingId) {
        MedlemskapPerioderEntitet nyPeriode = new MedlemskapPerioderBuilder()
                .medPeriode(datoMedEndring, null)
                .medDekningType(MedlemskapDekningType.FULL)
                .medMedlemskapType(MedlemskapType.ENDELIG)
                .medKildeType(MedlemskapKildeType.MEDL)
                .medMedlId(2L)
                .build();
        medlemskapRepository.lagreMedlemskapRegisterOpplysninger(behandlingId, List.of(periode, nyPeriode));
    }

    private Behandling opprettRevudering(Behandling behandling) {
        BehandlingÅrsak.Builder revurderingÅrsak = BehandlingÅrsak.builder(BehandlingÅrsakType.RE_FEIL_ELLER_ENDRET_FAKTA)
                .medOriginalBehandlingId(behandling.getId());

        Behandling revudering = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
                .medBehandlingÅrsak(revurderingÅrsak).build();

        Long behandlingId = behandling.getId();
        // TODO(FC): Her burde kanskje behandlingId vært låst inntil revurdering er
        // opprettet?
        behandlingRepository.lagre(revudering, behandlingRepository.taSkriveLås(revudering.getId()));
        Long revurderingId = revudering.getId();
        medlemskapRepository.kopierGrunnlagFraEksisterendeBehandling(behandlingId, revurderingId);
        iayTjeneste.kopierGrunnlagFraEksisterendeBehandling(behandlingId, revurderingId);
        personopplysningRepository.kopierGrunnlagFraEksisterendeBehandlingForRevurdering(behandlingId, revurderingId);

        return revudering;
    }

    private void avslutterBehandlingOgFagsak(Behandling behandling) {
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        provider.getFpUttakRepository().lagreOpprinneligUttakResultatPerioder(behandling.getId(), lagUttaksPeriode());

        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, lås);
        fagsakRepository.oppdaterFagsakStatus(behandling.getFagsakId(), FagsakStatus.AVSLUTTET);
    }

    private MedlemskapPerioderEntitet opprettPeriode(LocalDate fom, LocalDate tom, MedlemskapDekningType dekningType) {
        MedlemskapPerioderEntitet periode = new MedlemskapPerioderBuilder()
                .medDekningType(dekningType)
                .medMedlemskapType(MedlemskapType.FORELOPIG)
                .medPeriode(fom, tom)
                .medKildeType(MedlemskapKildeType.MEDL)
                .medMedlId(1L)
                .build();
        return periode;
    }

    private UttakResultatPerioderEntitet lagUttaksPeriode() {
        LocalDate idag = LocalDate.now();
        UttakResultatPeriodeEntitet periode = new UttakResultatPeriodeEntitet.Builder(idag, idag.plusDays(6))
                .medResultatType(PeriodeResultatType.INNVILGET, PeriodeResultatÅrsak.UKJENT)
                .build();
        UttakAktivitetEntitet uttakAktivtet = new UttakAktivitetEntitet.Builder()
                .medUttakArbeidType(UttakArbeidType.ORDINÆRT_ARBEID)
                .medArbeidsforhold(Arbeidsgiver.virksomhet("123"), InternArbeidsforholdRef.nyRef())
                .build();
        UttakResultatPeriodeAktivitetEntitet periodeAktivitet = new UttakResultatPeriodeAktivitetEntitet.Builder(periode, uttakAktivtet)
                .medUtbetalingsgrad(new Utbetalingsgrad(100))
                .medArbeidsprosent(BigDecimal.valueOf(100L))
                .medErSøktGradering(true)
                .medTrekkonto(StønadskontoType.MØDREKVOTE)
                .build();
        periode.leggTilAktivitet(periodeAktivitet);
        UttakResultatPerioderEntitet perioder = new UttakResultatPerioderEntitet();
        perioder.leggTilPeriode(periode);
        return perioder;
    }

}
