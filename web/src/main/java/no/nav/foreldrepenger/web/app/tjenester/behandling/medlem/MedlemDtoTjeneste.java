package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.OVERSTYRING_AV_FORUTGÅENDE_MEDLEMSKAPSVILKÅR;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.VURDER_MEDLEMSKAPSVILKÅRET;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
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
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.AbstractLocalDateInterval;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.MedlemskapAvvik;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.AvklarMedlemskapUtleder;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
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
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    @Inject
    public MedlemDtoTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider,
                             SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                             MedlemTjeneste medlemTjeneste,
                             PersonopplysningTjeneste personopplysningTjeneste,
                             AvklarMedlemskapUtleder medlemskapUtleder,
                             VilkårResultatRepository vilkårResultatRepository,
                             BehandlingskontrollTjeneste behandlingskontrollTjeneste) {

        this.medlemskapRepository = behandlingRepositoryProvider.getMedlemskapRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.medlemTjeneste = medlemTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.medlemskapUtleder = medlemskapUtleder;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
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
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        var legacyManuellBehandling = manuellBehandling.isEmpty() ? legacyManuellBehandling(ref, stp).orElse(null) : null;

        var aktørId = ref.aktørId();
        var regioner = personopplysningerAggregat.getStatsborgerskapRegionIInterval(aktørId, forPeriode, stp.getUtledetSkjæringstidspunkt())
            .stream()
            .map(s -> new MedlemskapDto.Region(s.getFom(), s.getTom(), s.getValue()))
            .collect(Collectors.toSet());

        var personstatuser = personopplysningerAggregat.getPersonstatuserFor(aktørId, forPeriode)
            .stream()
            .map(MedlemskapDto.Personstatus::map)
            .collect(Collectors.toSet());

        var medlemskap = medlemskapRepository.hentMedlemskap(ref.behandlingId());
        var utenlandsopphold = medlemskap.flatMap(MedlemskapAggregat::getOppgittTilknytning)
            .map(MedlemskapOppgittTilknytningEntitet::getOpphold)
            .orElse(Set.of())
            .stream()
            .map(MedlemskapDto.Utenlandsopphold::map)
            .collect(Collectors.toSet());

        var adresser = personopplysningerAggregat.getAdresserFor(aktørId, forPeriode)
            .stream()
            .map(MedlemskapDto.Adresse::map)
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

        return Optional.of(new MedlemskapDto(manuellBehandling.orElse(null), legacyManuellBehandling, regioner, personstatuser, utenlandsopphold, adresser,
            oppholdstillatelser, medlemskapsperioder, avvik, annenpart));
    }

    private Set<MedlemskapAvvik> utledAvvik(Behandling behandling) {
        if (behandlingLiggerEtterMedlemskapsvilkårssteg(behandling) && !aksjonspunktErOpprettetEllerLøst(behandling)) {
            return Set.of();
        }
        return medlemskapUtleder.utledAvvik(BehandlingReferanse.fra(behandling));
    }

    private Optional<MedlemskapDto.LegacyManuellBehandling> legacyManuellBehandling(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var vurdertMedlemskapEntitet = medlemskapRepository.hentLegacyVurderingMedlemskapSkjæringstidspunktet(ref.behandlingId());
        if (vurdertMedlemskapEntitet.isEmpty()) {
            return Optional.empty();
        }

        var medlemskapVurdering = vurdertMedlemskapEntitet.get();
        var perioder = new HashSet<MedlemskapDto.LegacyManuellBehandling.MedlemPeriode>();
        perioder.add(tilLegacyManuellBehandligPeriode(medlemskapVurdering, stp.getUtledetSkjæringstidspunkt()));

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
                var opphørsdato = v.getGjeldendeVilkårUtfall()
                    .equals(VilkårUtfallType.OPPFYLT) ? medlemTjeneste.hentOpphørsdatoHvisEksisterer(behandling.getId()).orElse(null) : null;
                var avslagskode = medlemTjeneste.hentAvslagsårsak(behandling.getId()).filter(å -> !å.equals(Avslagsårsak.UDEFINERT)).orElse(null);
                var medlemFom = medlemTjeneste.hentMedlemFomDato(behandling.getId()).orElse(null);
                return new MedlemskapDto.ManuellBehandlingResultat(avslagskode, medlemFom, opphørsdato);
            }));
    }

    private static boolean aksjonspunktErOpprettetEllerLøst(Behandling behandling) {
        return VURDER_MEDLEMSKAPSVILKÅRET_AKSJONSPUNKT
            .stream()
            .anyMatch(a -> behandling.harUtførtAksjonspunktMedType(a) || behandling.harÅpentAksjonspunktMedType(a));
    }

    private boolean behandlingLiggerEtterMedlemskapsvilkårssteg(Behandling behandling) {
        return behandlingskontrollTjeneste.erIStegEllerSenereSteg(behandling.getId(), BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR);
    }

    private static Optional<MedlemskapDto.Annenpart> annenpart(PersonopplysningerAggregat personopplysningerAggregat,
                                                               AbstractLocalDateInterval forPeriode,
                                                               Skjæringstidspunkt stp) {
        var annenpartOpt = personopplysningerAggregat.getOppgittAnnenPart()
            .map(OppgittAnnenPartEntitet::getAktørId)
            .or(() -> personopplysningerAggregat.getAnnenPartEllerEktefelle().map(PersonopplysningEntitet::getAktørId));

        if (annenpartOpt.isEmpty()) {
            return Optional.empty();
        }

        var adresser = personopplysningerAggregat.getAdresserFor(annenpartOpt.get(), forPeriode)
            .stream()
            .map(MedlemskapDto.Adresse::map)
            .collect(Collectors.toSet());

        var regioner = personopplysningerAggregat.getStatsborgerskapRegionIInterval(annenpartOpt.get(), forPeriode,
                stp.getUtledetSkjæringstidspunkt())
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
