package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_LOVLIG_OPPHOLD;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_OM_ER_BOSATT;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_OPPHOLDSRETT;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.VURDER_MEDLEMSKAPSVILKÅRET;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertLøpendeMedlemskapEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskap;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskapPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppgittAnnenPartEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppholdstillatelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.medlem.api.VurderMedlemskap;
import no.nav.foreldrepenger.domene.medlem.api.VurderingsÅrsak;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.AbstractLocalDateInterval;
import no.nav.foreldrepenger.domene.tid.SimpleLocalDateInterval;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.AvklarMedlemskapUtleder;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.v2.MedlemskapAvvik;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning.PersonopplysningDtoTjeneste;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class MedlemDtoTjeneste {

    private static final LocalDate OPPHOLD_CUTOFF = LocalDate.of(2018, 7, 1);

    private static final List<AksjonspunktDefinisjon> MEDL_AKSJONSPUNKTER = List.of(AVKLAR_OM_ER_BOSATT, AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE,
        AVKLAR_LOVLIG_OPPHOLD, AVKLAR_OPPHOLDSRETT);

    private MedlemskapRepository medlemskapRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BehandlingRepository behandlingRepository;
    private MedlemTjeneste medlemTjeneste;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private PersonopplysningDtoTjeneste personopplysningDtoTjeneste;
    private AvklarMedlemskapUtleder medlemskapUtleder;
    private VilkårResultatRepository vilkårResultatRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    @Inject
    public MedlemDtoTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider,
                             SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                             MedlemTjeneste medlemTjeneste,
                             PersonopplysningTjeneste personopplysningTjeneste,
                             PersonopplysningDtoTjeneste personopplysningDtoTjeneste,
                             AvklarMedlemskapUtleder medlemskapUtleder,
                             VilkårResultatRepository vilkårResultatRepository,
                             BehandlingskontrollTjeneste behandlingskontrollTjeneste) {

        this.medlemskapRepository = behandlingRepositoryProvider.getMedlemskapRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.medlemTjeneste = medlemTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.personopplysningDtoTjeneste = personopplysningDtoTjeneste;
        this.medlemskapUtleder = medlemskapUtleder;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
    }

    MedlemDtoTjeneste() {
        // CDI
    }

    private static List<MedlemskapPerioderDto> lagMedlemskapPerioderDto(Set<MedlemskapPerioderEntitet> perioder) {
        return perioder.stream().map(mp -> {
            var dto = new MedlemskapPerioderDto();
            dto.setFom(mp.getFom());
            dto.setTom(mp.getTom());
            dto.setMedlemskapType(mp.getMedlemskapType());
            dto.setKildeType(mp.getKildeType());
            dto.setDekningType(mp.getDekningType());
            dto.setBeslutningsdato(mp.getBeslutningsdato());
            return dto;
        }).toList();
    }

    public MedlemskapV3Dto lagMedlemskap(UUID behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        var ref = BehandlingReferanse.fra(behandling);

        var po = personopplysningTjeneste.hentPersonopplysningerHvisEksisterer(ref);

        if (po.isEmpty()) {
            return null;
        }
        var personopplysningerAggregat = po.get();
        var forPeriode = SimpleLocalDateInterval.fraOgMedTomNotNull(Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE);

        var manuellBehandling = manuellBehandling(behandling);
        var legacyManuellBehandling = manuellBehandling.isEmpty() ? legacyManuellBehandling(ref, stp).orElse(null) : null;

        var aktørId = ref.aktørId();
        var regioner = personopplysningerAggregat.getStatsborgerskapRegionIInterval(aktørId, forPeriode, stp.getUtledetSkjæringstidspunkt())
            .stream()
            .map(s -> new MedlemskapV3Dto.Region(s.getFom(), s.getTom(), s.getValue()))
            .collect(Collectors.toSet());

        var personstatuser = personopplysningerAggregat.getPersonstatuserFor(aktørId, forPeriode)
            .stream()
            .map(MedlemskapV3Dto.Personstatus::map)
            .collect(Collectors.toSet());

        var medlemskap = medlemskapRepository.hentMedlemskap(ref.behandlingId());
        var utenlandsopphold = medlemskap.flatMap(MedlemskapAggregat::getOppgittTilknytning)
            .map(MedlemskapOppgittTilknytningEntitet::getOpphold)
            .orElse(Set.of())
            .stream()
            .map(MedlemskapV3Dto.Utenlandsopphold::map)
            .collect(Collectors.toSet());

        var adresser = personopplysningerAggregat.getAdresserFor(aktørId, forPeriode)
            .stream()
            .map(MedlemskapV3Dto.Adresse::map)
            .collect(Collectors.toSet());

        var oppholdstillatelser = personopplysningerAggregat.getOppholdstillatelseFor(aktørId, forPeriode)
            .stream()
            .map(MedlemskapV3Dto.Oppholdstillatelse::map)
            .collect(Collectors.toSet());

        var medlemskapsperioder = medlemskap.map(MedlemskapAggregat::getRegistrertMedlemskapPerioder)
            .orElse(Set.of())
            .stream()
            .map(MedlemskapV3Dto.MedlemskapPeriode::map)
            .collect(Collectors.toSet());

        var annenpart = annenpart(personopplysningerAggregat, forPeriode, stp).orElse(null);
        var avvik = utledAvvik(behandling);

        return new MedlemskapV3Dto(manuellBehandling.orElse(null), legacyManuellBehandling, regioner, personstatuser, utenlandsopphold, adresser,
            oppholdstillatelser, medlemskapsperioder, avvik, annenpart);
    }

    private Set<MedlemskapAvvik> utledAvvik(Behandling behandling) {
        if (behandlingLiggerEtterMedlemskapsvilkårssteg(behandling) && !aksjonspunktErOpprettetEllerLøst(behandling)) {
            return Set.of();
        }
        return medlemskapUtleder.utledAvvik(BehandlingReferanse.fra(behandling));
    }

    private Optional<MedlemskapV3Dto.LegacyManuellBehandling> legacyManuellBehandling(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var medlemskapOpt = medlemskapRepository.hentMedlemskap(ref.behandlingId());
        if (medlemskapOpt.isEmpty() || medlemskapOpt.get().getVurdertMedlemskap().isEmpty()) {
            return Optional.empty();
        }

        var medlemskapAggregat = medlemskapOpt.get();
        var vurdertMedlemskap = medlemskapAggregat.getVurdertMedlemskap().get();
        var perioder = new HashSet<MedlemskapV3Dto.LegacyManuellBehandling.MedlemPeriode>();
        perioder.add(tilLegacyManuellBehandligPeriode(vurdertMedlemskap, stp.getUtledetSkjæringstidspunkt()));

        if (medlemskapAggregat.getVurderingLøpendeMedlemskap().isPresent()) {
            perioder.addAll(medlemskapAggregat.getVurderingLøpendeMedlemskap()
                .get()
                .getPerioder()
                .stream()
                .map(v -> tilLegacyManuellBehandligPeriode(v, v.getVurderingsdato()))
                .collect(Collectors.toSet()));
        }

        return Optional.of(new MedlemskapV3Dto.LegacyManuellBehandling(perioder));
    }

    private static MedlemskapV3Dto.LegacyManuellBehandling.MedlemPeriode tilLegacyManuellBehandligPeriode(VurdertMedlemskap vurdertMedlemskap,
                                                                                                          LocalDate vurderingsdato) {
        return new MedlemskapV3Dto.LegacyManuellBehandling.MedlemPeriode(vurderingsdato, vurdertMedlemskap.getOppholdsrettVurdering(),
            vurdertMedlemskap.getErEøsBorger(), vurdertMedlemskap.getLovligOppholdVurdering(), vurdertMedlemskap.getBosattVurdering(),
            vurdertMedlemskap.getMedlemsperiodeManuellVurdering(), vurdertMedlemskap.getBegrunnelse());
    }

    private Optional<MedlemskapV3Dto.ManuellBehandlingResultat> manuellBehandling(Behandling behandling) {
        var medlemskapsvilkår = vilkårResultatRepository.hentHvisEksisterer(behandling.getId())
            .stream()
            .flatMap(vr -> vr.getVilkårene().stream())
            .filter(v -> v.getVilkårType().equals(VilkårType.MEDLEMSKAPSVILKÅRET)) //TODO forutgående
            .findFirst();
        return medlemskapsvilkår.filter(m -> VilkårUtfallType.erFastsatt(m.getVilkårUtfallManuelt())).map((v -> {
            Optional<LocalDate> opphørsdato = v.getGjeldendeVilkårUtfall()
                .equals(VilkårUtfallType.OPPFYLT) ? medlemTjeneste.hentOpphørsdatoHvisEksisterer(behandling.getId()) : Optional.empty();
            var avslagskode = medlemTjeneste.hentAvslagsårsak(behandling.getId()).filter(å -> !å.equals(Avslagsårsak.UDEFINERT));
            return new MedlemskapV3Dto.ManuellBehandlingResultat(avslagskode.orElse(null), null, opphørsdato.orElse(null));
            }));
    }

    private static boolean aksjonspunktErOpprettetEllerLøst(Behandling behandling) {
        return Set.of(VURDER_MEDLEMSKAPSVILKÅRET) //TODO forutgående
            .stream()
            .anyMatch(a -> behandling.harUtførtAksjonspunktMedType(a) || behandling.harÅpentAksjonspunktMedType(a));
    }

    private boolean behandlingLiggerEtterMedlemskapsvilkårssteg(Behandling behandling) {
        return behandlingskontrollTjeneste.erIStegEllerSenereSteg(behandling.getId(), BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR);
    }

    private static Optional<MedlemskapV3Dto.Annenpart> annenpart(PersonopplysningerAggregat personopplysningerAggregat,
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
            .map(MedlemskapV3Dto.Adresse::map)
            .collect(Collectors.toSet());

        var regioner = personopplysningerAggregat.getStatsborgerskapRegionIInterval(annenpartOpt.get(), forPeriode,
                stp.getUtledetSkjæringstidspunkt())
            .stream()
            .map(s -> new MedlemskapV3Dto.Region(s.getFom(), s.getTom(), s.getValue()))
            .collect(Collectors.toSet());

        var personstatus = personopplysningerAggregat.getPersonstatuserFor(annenpartOpt.get(), forPeriode)
            .stream()
            .map(MedlemskapV3Dto.Personstatus::map)
            .collect(Collectors.toSet());

        return Optional.of(new MedlemskapV3Dto.Annenpart(adresser, regioner, personstatus));
    }

    public Optional<MedlemV2Dto> lagMedlemV2Dto(Long behandlingId) {
        var medlemskapOpt = medlemskapRepository.hentMedlemskap(behandlingId);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var dto = new MedlemV2Dto();
        var ref = BehandlingReferanse.fra(behandling);
        var stp = finnSkjæringstidspunktHvisTilgjengelig(behandlingId);
        var personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysningerHvisEksisterer(ref);
        // Tomt innhold hvis det mangler behandlingsgrunnlag.
        if (personopplysningerAggregat.map(PersonopplysningerAggregat::getSøker).isPresent()) {
            stp.ifPresent(s -> mapSkjæringstidspunkt(dto, medlemskapOpt.orElse(null), behandling.getAksjonspunkter(), ref, s));
            mapRegistrerteMedlPerioder(dto, medlemskapOpt.map(MedlemskapAggregat::getRegistrertMedlemskapPerioder).orElse(Collections.emptySet()));
            dto.setOpphold(mapOppholdstillatelser(behandlingId));
            dto.setFom(mapMedlemV2Fom(behandling, ref, stp, personopplysningerAggregat, medlemskapOpt).orElse(null));

            if (behandling.harAksjonspunktMedType(AksjonspunktDefinisjon.AVKLAR_FORTSATT_MEDLEMSKAP)) {
                mapAndrePerioder(dto, medlemskapOpt.flatMap(MedlemskapAggregat::getVurderingLøpendeMedlemskap)
                    .map(VurdertMedlemskapPeriodeEntitet::getPerioder)
                    .orElse(Collections.emptySet()), ref, stp);
            }
        }
        return Optional.of(dto);
    }

    private Optional<Skjæringstidspunkt> finnSkjæringstidspunktHvisTilgjengelig(Long behandlingId) {
        try {
            return Optional.ofNullable(skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /*
     * Skal finne evt opphør FOM dato for revurderinger
     */
    private Optional<LocalDate> mapMedlemV2Fom(Behandling behandling,
                                               BehandlingReferanse ref,
                                               Optional<Skjæringstidspunkt> stp,
                                               Optional<PersonopplysningerAggregat> personopplysningerAggregat,
                                               Optional<MedlemskapAggregat> medlemskapAggregat) {
        var fom = medlemV2FomFraMedlemskap(stp, medlemskapAggregat);
        // TODO Fom fra personopplysninger kun revurdering og foreldrepenger, bør skilles ut som egen DTO for FP+BT-004
        if (personopplysningerAggregat.isPresent()) {
            var endringerIPersonopplysninger = medlemTjeneste.søkerHarEndringerIPersonopplysninger(behandling, ref);
            var endredeAttributter = endringerIPersonopplysninger.getEndredeAttributter();
            if (!endredeAttributter.isEmpty()) {
                return endringerIPersonopplysninger.getGjeldendeFra();
            }
            if (stp.map(Skjæringstidspunkt::getUtledetSkjæringstidspunkt).or(() -> fom).isEmpty()) {
                return fom;
            }
            var forPeriode = SimpleLocalDateInterval.enDag(stp.map(Skjæringstidspunkt::getUtledetSkjæringstidspunkt).or(() -> fom).orElseThrow());
            /* Ingen endringer i personopplysninger (siden siste vedtatte medlemskapsperiode),
            så vi setter gjeldende f.o.m fra nyeste endring i personstatus. Denne vises b.a. ifm. aksjonspunkt 5022 */
            if (fom.isPresent() && personopplysningerAggregat.get().getPersonstatusFor(ref.aktørId(), forPeriode) != null && fom.get()
                .isBefore(personopplysningerAggregat.get().getPersonstatusFor(ref.aktørId(), forPeriode).getPeriode().getFomDato())) {
                return Optional.ofNullable(personopplysningerAggregat.get().getPersonstatusFor(ref.aktørId(), forPeriode).getPeriode().getFomDato());

            }
        }
        return fom;
    }

    private Optional<LocalDate> medlemV2FomFraMedlemskap(Optional<Skjæringstidspunkt> skjæringstidspunkt,
                                                         Optional<MedlemskapAggregat> medlemskapAggregat) {
        return medlemskapAggregat.flatMap(MedlemskapAggregat::getVurdertMedlemskap).isPresent() ? skjæringstidspunkt.flatMap(
            Skjæringstidspunkt::getSkjæringstidspunktHvisUtledet) : Optional.empty();
    }

    private void mapRegistrerteMedlPerioder(MedlemV2Dto dto, Set<MedlemskapPerioderEntitet> perioder) {
        dto.setMedlemskapPerioder(lagMedlemskapPerioderDto(perioder));
    }

    private void mapAndrePerioder(MedlemV2Dto dto,
                                  Set<VurdertLøpendeMedlemskapEntitet> perioder,
                                  BehandlingReferanse ref,
                                  Optional<Skjæringstidspunkt> stp) {
        if (stp.isEmpty()) {
            return;
        }
        var vurderingspunkter = medlemTjeneste.utledVurderingspunkterMedAksjonspunkt(ref, stp.get());
        var dtoPerioder = dto.getPerioder();
        for (var entrySet : vurderingspunkter.entrySet()) {
            var vurdertMedlemskap = finnVurderMedlemskap(perioder, entrySet);
            var medlemPeriodeDto = mapTilPeriodeDto(ref, stp.get(), vurdertMedlemskap, entrySet.getKey(), entrySet.getValue().årsaker(),
                vurdertMedlemskap.map(VurdertMedlemskap::getBegrunnelse).orElse(null));
            medlemPeriodeDto.setAksjonspunkter(entrySet.getValue().aksjonspunkter().stream().map(Kodeverdi::getKode).collect(Collectors.toSet()));
            dtoPerioder.add(medlemPeriodeDto);
        }
    }

    private List<OppholdstillatelseDto> mapOppholdstillatelser(Long behandlingId) {
        return personopplysningTjeneste.hentOppholdstillatelser(behandlingId).stream().map(this::mapOppholdstillatelse).toList();
    }

    private OppholdstillatelseDto mapOppholdstillatelse(OppholdstillatelseEntitet oppholdstillatelse) {
        var dto = new OppholdstillatelseDto();
        dto.setOppholdstillatelseType(oppholdstillatelse.getTillatelse());
        dto.setFom(oppholdstillatelse.getPeriode().getFomDato().isBefore(OPPHOLD_CUTOFF) ? null : oppholdstillatelse.getPeriode().getFomDato());
        dto.setTom(oppholdstillatelse.getPeriode().getTomDato());
        return dto;
    }

    private Optional<VurdertMedlemskap> finnVurderMedlemskap(Set<VurdertLøpendeMedlemskapEntitet> perioder,
                                                             Map.Entry<LocalDate, VurderMedlemskap> entrySet) {
        return perioder.stream().filter(it -> it.getVurderingsdato().equals(entrySet.getKey())).map(it -> (VurdertMedlemskap) it).findAny();
    }

    private void mapSkjæringstidspunkt(MedlemV2Dto dto,
                                       MedlemskapAggregat aggregat,
                                       Set<Aksjonspunkt> aksjonspunkter,
                                       BehandlingReferanse ref,
                                       Skjæringstidspunkt stp) {
        var aggregatOpts = Optional.ofNullable(aggregat);
        var vurdertMedlemskapOpt = aggregatOpts.flatMap(MedlemskapAggregat::getVurdertMedlemskap);
        var vurderingsdato = stp.getSkjæringstidspunktHvisUtledet().orElse(null);
        var begrunnelse = vurdertMedlemskapOpt.map(VurdertMedlemskap::getBegrunnelse).orElseGet(() -> hentBegrunnelseFraAksjonspuntk(aksjonspunkter));
        var periodeDto = mapTilPeriodeDto(ref, stp, vurdertMedlemskapOpt, vurderingsdato, Set.of(VurderingsÅrsak.SKJÆRINGSTIDSPUNKT), begrunnelse);
        periodeDto.setAksjonspunkter(aksjonspunkter.stream()
            .map(Aksjonspunkt::getAksjonspunktDefinisjon)
            .filter(MEDL_AKSJONSPUNKTER::contains)
            .map(Kodeverdi::getKode)
            .collect(Collectors.toSet()));
        dto.getPerioder().add(periodeDto);
    }

    private MedlemPeriodeDto mapTilPeriodeDto(BehandlingReferanse ref,
                                              Skjæringstidspunkt stp,
                                              Optional<VurdertMedlemskap> vurdertMedlemskapOpt,
                                              LocalDate vurderingsdato,
                                              Set<VurderingsÅrsak> årsaker,
                                              String begrunnelse) {
        var periodeDto = new MedlemPeriodeDto();
        periodeDto.setÅrsaker(årsaker);
        personopplysningDtoTjeneste.lagPersonopplysningMedlemskapDto(ref, stp, vurderingsdato).ifPresent(periodeDto::setPersonopplysningBruker);
        personopplysningDtoTjeneste.lagAnnenpartPersonopplysningMedlemskapDto(ref, stp, vurderingsdato)
            .ifPresent(periodeDto::setPersonopplysningAnnenPart);
        periodeDto.setVurderingsdato(vurderingsdato);
        periodeDto.setBegrunnelse(begrunnelse);

        if (vurdertMedlemskapOpt.isPresent()) {
            var vurdertMedlemskap = vurdertMedlemskapOpt.get();
            periodeDto.setBosattVurdering(vurdertMedlemskap.getBosattVurdering());
            periodeDto.setOppholdsrettVurdering(vurdertMedlemskap.getOppholdsrettVurdering());
            periodeDto.setLovligOppholdVurdering(vurdertMedlemskap.getLovligOppholdVurdering());
            periodeDto.setErEosBorger(vurdertMedlemskap.getErEøsBorger());
            periodeDto.setMedlemskapManuellVurderingType(vurdertMedlemskap.getMedlemsperiodeManuellVurdering());
            periodeDto.setBegrunnelse(vurdertMedlemskap.getBegrunnelse());
        }
        return periodeDto;
    }

    //TODO(OJR) Hack!!! kan fjernes hvis man ønsker å utføre en migrerning(kompleks) av gamle medlemskapvurdering i produksjon
    private String hentBegrunnelseFraAksjonspuntk(Set<Aksjonspunkt> aksjonspunkter) {
        return aksjonspunkter.stream()
            .filter(a -> VilkårType.MEDLEMSKAPSVILKÅRET.equals(a.getAksjonspunktDefinisjon().getVilkårType()))
            .findFirst()
            .map(Aksjonspunkt::getBegrunnelse)
            .orElse(null);
    }
}
