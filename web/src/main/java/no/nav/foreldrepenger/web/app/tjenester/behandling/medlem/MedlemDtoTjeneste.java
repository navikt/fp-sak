package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.OVERSTYRING_AV_FORUTGÅENDE_MEDLEMSKAPSVILKÅR;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.VURDER_MEDLEMSKAPSVILKÅRET;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskap;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.AbstractLocalDateInterval;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.MedlemskapAvvik;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.AvklarMedlemskapUtleder;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning.PersonadresseDto;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class MedlemDtoTjeneste {
    private static final Set<AksjonspunktDefinisjon> VURDER_MEDLEMSKAPSVILKÅRET_AKSJONSPUNKT = Set.of(VURDER_MEDLEMSKAPSVILKÅRET,
        VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR, OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET, OVERSTYRING_AV_FORUTGÅENDE_MEDLEMSKAPSVILKÅR);

    private MedlemskapRepository medlemskapRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BehandlingRepository behandlingRepository;
    private MedlemTjeneste medlemTjeneste;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private AvklarMedlemskapUtleder medlemskapUtleder;
    private VilkårResultatRepository vilkårResultatRepository;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;

    @Inject
    public MedlemDtoTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider,
                             SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                             MedlemTjeneste medlemTjeneste,
                             PersonopplysningTjeneste personopplysningTjeneste,
                             AvklarMedlemskapUtleder medlemskapUtleder,
                             VilkårResultatRepository vilkårResultatRepository,
                             BehandlingProsesseringTjeneste behandlingProsesseringTjeneste) {

        this.medlemskapRepository = behandlingRepositoryProvider.getMedlemskapRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.medlemTjeneste = medlemTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.medlemskapUtleder = medlemskapUtleder;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
    }

    MedlemDtoTjeneste() {
        // CDI
    }

    public Optional<MedlemskapDto> lagMedlemskap(UUID behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);

        var po = personopplysningTjeneste.hentPersonopplysningerHvisEksisterer(ref);

        if (po.isEmpty() || !po.get().harInnhentetPersonopplysningerFraRegister()) {
            return Optional.empty();
        }
        var personopplysningerAggregat = po.get();
        var forPeriode = SimpleLocalDateInterval.fraOgMedTomNotNull(Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE);

        var manuellBehandling = manuellBehandling(behandling);
        var stp = failsoftSkjæringstidspunkt(behandling.getId()).orElseGet(LocalDate::now);
        var legacyManuellBehandling = manuellBehandling.isEmpty() ? legacyManuellBehandling(ref, stp).orElse(null) : null;

        var aktørId = ref.aktørId();
        var regioner = personopplysningerAggregat.getStatsborgerskapRegionIInterval(aktørId, forPeriode, stp)
            .stream()
            .map(s -> new MedlemskapDto.Region(s.getFom(), s.getTom(), s.getValue()))
            .collect(Collectors.toSet());

        var personstatuser = personopplysningerAggregat.getPersonstatuserFor(aktørId, forPeriode)
            .stream()
            .map(MedlemskapDto.Personstatus::map)
            .collect(Collectors.toSet());

        var medlemskap = medlemskapRepository.hentMedlemskap(ref.behandlingId());

        var adresser = personopplysningerAggregat.getAdresserFor(aktørId, forPeriode)
            .stream()
            .map(PersonadresseDto::tilDto)
            .collect(Collectors.toSet());

        var oppholdstillatelser = personopplysningerAggregat.getOppholdstillatelseFor(aktørId, forPeriode)
            .stream()
            .map(MedlemskapDto.Oppholdstillatelse::map)
            .collect(Collectors.toSet());

        var medlemskapsperioder = medlemskap.map(MedlemskapAggregat::getRegistrertMedlemskapPerioder)
            .orElse(Set.of())
            .stream()
            .map(MedlemskapDto.MedlemskapPeriode::map)
            .collect(Collectors.toSet());

        var annenpart = annenpart(personopplysningerAggregat, forPeriode, stp).orElse(null);
        var avvik = utledAvvik(behandling);

        var oppgittUtenlandsopphold = medlemskap.flatMap(MedlemskapAggregat::getOppgittTilknytning).map(m -> {
            var utlandsoppholdFør = mapUtenlandsperiode(m.getOpphold(), MedlemskapOppgittLandOppholdEntitet::isTidligereOpphold);
            var utlandsoppholdEtter = mapUtenlandsperiode(m.getOpphold(),
                medlemskapOppgittLandOppholdEntitet -> !medlemskapOppgittLandOppholdEntitet.isTidligereOpphold());
            return new MedlemskapDto.OppgittUtlandsopphold(m.isOppholdINorgeSistePeriode(), m.isOppholdINorgeNestePeriode(), utlandsoppholdFør,
                utlandsoppholdEtter);
        }).orElse(null);

        return Optional.of(
            new MedlemskapDto(manuellBehandling.orElse(null), legacyManuellBehandling, regioner, personstatuser, oppgittUtenlandsopphold,
                adresser, oppholdstillatelser, medlemskapsperioder, avvik, annenpart));
    }

    private List<MedlemskapDto.Utlandsopphold> mapUtenlandsperiode(Set<MedlemskapOppgittLandOppholdEntitet> opphold,
                                                                   Predicate<MedlemskapOppgittLandOppholdEntitet> filter) {
        return opphold.stream()
            .filter(filter)
            .filter(o -> !o.getLand().equals(Landkoder.NOR))
            .map(MedlemskapDto.Utlandsopphold::map)
            .toList();
    }

    private Optional<LocalDate> failsoftSkjæringstidspunkt(Long behandlingId) {
        try {
            return skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId).getSkjæringstidspunktHvisUtledet();
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private Set<MedlemskapAvvik> utledAvvik(Behandling behandling) {
        if (behandling.erAvsluttet() || (behandlingLiggerEtterMedlemskapsvilkårssteg(behandling) && !aksjonspunktErOpprettetEllerLøst(behandling))) {
            return Set.of();
        }
        return medlemskapUtleder.utledAvvik(BehandlingReferanse.fra(behandling));
    }

    private Optional<MedlemskapDto.LegacyManuellBehandling> legacyManuellBehandling(BehandlingReferanse ref, LocalDate stp) {
        var vurdertMedlemskapEntitet = medlemskapRepository.hentLegacyVurderingMedlemskapSkjæringstidspunktet(ref.behandlingId());
        if (vurdertMedlemskapEntitet.isEmpty()) {
            return Optional.empty();
        }

        var medlemskapVurdering = vurdertMedlemskapEntitet.get();
        var perioder = new HashSet<MedlemskapDto.LegacyManuellBehandling.MedlemPeriode>();
        perioder.add(tilLegacyManuellBehandligPeriode(medlemskapVurdering, stp));

        var løpendeVurdering = medlemskapRepository.hentLegacyVurderingLøpendeMedlemskap(ref.behandlingId());
        if (løpendeVurdering.isPresent()) {
            perioder.addAll(løpendeVurdering.get()
                .getPerioder()
                .stream()
                .map(v -> tilLegacyManuellBehandligPeriode(v, v.getVurderingsdato()))
                .collect(Collectors.toSet()));
        }

        return Optional.of(new MedlemskapDto.LegacyManuellBehandling(perioder));
    }

    private static MedlemskapDto.LegacyManuellBehandling.MedlemPeriode tilLegacyManuellBehandligPeriode(VurdertMedlemskap vurdertMedlemskap,
                                                                                                        LocalDate vurderingsdato) {
        return new MedlemskapDto.LegacyManuellBehandling.MedlemPeriode(vurderingsdato, vurdertMedlemskap.getOppholdsrettVurdering(),
            vurdertMedlemskap.getErEøsBorger(), vurdertMedlemskap.getLovligOppholdVurdering(), vurdertMedlemskap.getBosattVurdering(),
            vurdertMedlemskap.getMedlemsperiodeManuellVurdering(), vurdertMedlemskap.getBegrunnelse());
    }

    private Optional<MedlemskapDto.ManuellBehandlingResultat> manuellBehandling(Behandling behandling) {
        if (VURDER_MEDLEMSKAPSVILKÅRET_AKSJONSPUNKT.stream().anyMatch(behandling::harÅpentAksjonspunktMedType)) {
            return Optional.empty();
        }

        return vilkårResultatRepository.hentHvisEksisterer(behandling.getId())
            .stream()
            .flatMap(vr -> vr.getVilkårene().stream())
            .filter(v -> v.getVilkårType().gjelderMedlemskap())
            .filter(v -> v.erManueltVurdert() || v.erOverstyrt())
            .findFirst()
            .map((v -> {
                var opphørsdato = v.getGjeldendeVilkårUtfall().equals(VilkårUtfallType.OPPFYLT) ? medlemTjeneste.hentOpphørsdatoHvisEksisterer(
                    behandling.getId()).orElse(null) : null;
                var avslagskode = medlemTjeneste.hentAvslagsårsak(behandling.getId()).filter(å -> !å.equals(Avslagsårsak.UDEFINERT)).orElse(null);
                var medlemFom = medlemTjeneste.hentMedlemFomDato(behandling.getId()).orElse(null);
                return new MedlemskapDto.ManuellBehandlingResultat(avslagskode, medlemFom, opphørsdato);
            }));
    }

    private static boolean aksjonspunktErOpprettetEllerLøst(Behandling behandling) {
        return VURDER_MEDLEMSKAPSVILKÅRET_AKSJONSPUNKT.stream()
            .anyMatch(a -> behandling.harUtførtAksjonspunktMedType(a) || behandling.harÅpentAksjonspunktMedType(a));
    }

    private boolean behandlingLiggerEtterMedlemskapsvilkårssteg(Behandling behandling) {
        return !behandlingProsesseringTjeneste.erBehandlingFørSteg(behandling, BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR);
    }

    private static Optional<MedlemskapDto.Annenpart> annenpart(PersonopplysningerAggregat personopplysningerAggregat,
                                                               AbstractLocalDateInterval forPeriode,
                                                               LocalDate stp) {
        var annenpartOpt = personopplysningerAggregat.getOppgittAnnenPart()
            .map(OppgittAnnenPartEntitet::getAktørId)
            .or(() -> personopplysningerAggregat.getAnnenPartEllerEktefelle().map(PersonopplysningEntitet::getAktørId));

        if (annenpartOpt.isEmpty()) {
            return Optional.empty();
        }

        var adresser = personopplysningerAggregat.getAdresserFor(annenpartOpt.get(), forPeriode)
            .stream()
            .map(PersonadresseDto::tilDto)
            .collect(Collectors.toSet());

        var regioner = personopplysningerAggregat.getStatsborgerskapRegionIInterval(annenpartOpt.get(), forPeriode, stp)
            .stream()
            .map(s -> new MedlemskapDto.Region(s.getFom(), s.getTom(), s.getValue()))
            .collect(Collectors.toSet());

        var personstatus = personopplysningerAggregat.getPersonstatuserFor(annenpartOpt.get(), forPeriode)
            .stream()
            .map(MedlemskapDto.Personstatus::map)
            .collect(Collectors.toSet());

        return Optional.of(new MedlemskapDto.Annenpart(adresser, regioner, personstatus));
    }
}
