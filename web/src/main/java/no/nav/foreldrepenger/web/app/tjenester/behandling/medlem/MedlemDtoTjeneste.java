package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_LOVLIG_OPPHOLD;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_OM_ER_BOSATT;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_OPPHOLDSRETT;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertLøpendeMedlemskapEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskap;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskapPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.OppholdstillatelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.medlem.api.VurderMedlemskap;
import no.nav.foreldrepenger.domene.medlem.api.VurderingsÅrsak;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning.PersonopplysningDtoTjeneste;

@ApplicationScoped
public class MedlemDtoTjeneste {

    private static final LocalDate OPPHOLD_CUTOFF = LocalDate.of(2018,7,1);

    private static final List<AksjonspunktDefinisjon> MEDL_AKSJONSPUNKTER = List.of(AVKLAR_OM_ER_BOSATT,
        AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE,
        AVKLAR_LOVLIG_OPPHOLD,
        AVKLAR_OPPHOLDSRETT);

    private MedlemskapRepository medlemskapRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BehandlingRepository behandlingRepository;
    private MedlemTjeneste medlemTjeneste;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private PersonopplysningDtoTjeneste personopplysningDtoTjeneste;

    @Inject
    public MedlemDtoTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider,
                             SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                             MedlemTjeneste medlemTjeneste,
                             PersonopplysningTjeneste personopplysningTjeneste,
                             PersonopplysningDtoTjeneste personopplysningDtoTjeneste) {

        this.medlemskapRepository = behandlingRepositoryProvider.getMedlemskapRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.medlemTjeneste = medlemTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.personopplysningDtoTjeneste = personopplysningDtoTjeneste;
    }

    MedlemDtoTjeneste() {
        // CDI
    }

    private static List<MedlemskapPerioderDto> lagMedlemskapPerioderDto(Set<MedlemskapPerioderEntitet> perioder) {
        return perioder.stream().map(mp -> {
            MedlemskapPerioderDto dto = new MedlemskapPerioderDto();
            dto.setFom(mp.getFom());
            dto.setTom(mp.getTom());
            dto.setMedlemskapType(mp.getMedlemskapType());
            dto.setKildeType(mp.getKildeType());
            dto.setDekningType(mp.getDekningType());
            dto.setBeslutningsdato(mp.getBeslutningsdato());
            return dto;
        }).collect(Collectors.toList());
    }

    public Optional<MedlemV2Dto> lagMedlemV2Dto(Long behandlingId) {
        Optional<MedlemskapAggregat> medlemskapOpt = medlemskapRepository.hentMedlemskap(behandlingId);
        final Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        final MedlemV2Dto dto = new MedlemV2Dto();
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()));
        Optional<PersonopplysningerAggregat> personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysningerHvisEksisterer(ref);
        mapSkjæringstidspunkt(dto, medlemskapOpt.orElse(null), behandling.getAksjonspunkter(), ref);
        mapRegistrerteMedlPerioder(dto, medlemskapOpt.map(MedlemskapAggregat::getRegistrertMedlemskapPerioder).orElse(Collections.emptySet()));
        dto.setOpphold(mapOppholdstillatelser(behandlingId));
        dto.setFom(mapMedlemV2Fom(behandling, ref, personopplysningerAggregat, medlemskapOpt).orElse(null));

        if (behandling.getAksjonspunkter().stream().map(Aksjonspunkt::getAksjonspunktDefinisjon).collect(Collectors.toList()).contains(AksjonspunktDefinisjon.AVKLAR_FORTSATT_MEDLEMSKAP)) {
            mapAndrePerioder(dto, medlemskapOpt.flatMap(MedlemskapAggregat::getVurderingLøpendeMedlemskap).map(VurdertMedlemskapPeriodeEntitet::getPerioder).orElse(Collections.emptySet()), ref);
        }
        return Optional.of(dto);
    }

    /*
     * Skal finne evt opphør FOM dato for revurderinger
     */
    private Optional<LocalDate> mapMedlemV2Fom(Behandling behandling, BehandlingReferanse ref,
                                               Optional<PersonopplysningerAggregat> personopplysningerAggregat,
                                               Optional<MedlemskapAggregat> medlemskapAggregat) {
        var fom = medlemV2FomFraMedlemskap(ref, medlemskapAggregat);
        // TODO Fom fra personopplysninger kun revurdering og foreldrepenger, bør skilles ut som egen DTO for FP+BT-004
        if (personopplysningerAggregat.isPresent()) {
            var endringerIPersonopplysninger = medlemTjeneste.søkerHarEndringerIPersonopplysninger(behandling);
            var endredeAttributter = endringerIPersonopplysninger.getEndredeAttributter();
            if (!endredeAttributter.isEmpty()) {
                return endringerIPersonopplysninger.getGjeldendeFra();
            }

            /* Ingen endringer i personopplysninger (siden siste vedtatte medlemskapsperiode),
            så vi setter gjeldende f.o.m fra nyeste endring i personstatus. Denne vises b.a. ifm. aksjonspunkt 5022 */
            if (fom.isPresent() && personopplysningerAggregat.get().getPersonstatusFor(ref.getAktørId()) != null) {
                if (fom.get().isBefore(personopplysningerAggregat.get().getPersonstatusFor(ref.getAktørId()).getPeriode().getFomDato())) {
                    return Optional.ofNullable(personopplysningerAggregat.get().getPersonstatusFor(ref.getAktørId()).getPeriode().getFomDato());
                }
            }
        }
        return fom;
    }

    private Optional<LocalDate> medlemV2FomFraMedlemskap(BehandlingReferanse ref, Optional<MedlemskapAggregat> medlemskapAggregat) {
        return medlemskapAggregat.flatMap(MedlemskapAggregat::getVurdertMedlemskap).isPresent() ? ref.getSkjæringstidspunkt().getSkjæringstidspunktHvisUtledet() : Optional.empty();
    }

    private void mapRegistrerteMedlPerioder(MedlemV2Dto dto, Set<MedlemskapPerioderEntitet> perioder) {
        dto.setMedlemskapPerioder(lagMedlemskapPerioderDto(perioder));
    }

    private void mapAndrePerioder(MedlemV2Dto dto, Set<VurdertLøpendeMedlemskapEntitet> perioder, BehandlingReferanse ref) {
        final Map<LocalDate, VurderMedlemskap> vurderingspunkter = medlemTjeneste.utledVurderingspunkterMedAksjonspunkt(ref);
        final Set<MedlemPeriodeDto> dtoPerioder = dto.getPerioder();
        for (Map.Entry<LocalDate, VurderMedlemskap> entrySet : vurderingspunkter.entrySet()) {
            var vurdertMedlemskap = finnVurderMedlemskap(perioder, entrySet);
            final MedlemPeriodeDto medlemPeriodeDto = mapTilPeriodeDto(ref.getBehandlingId(),
                vurdertMedlemskap, entrySet.getKey(), entrySet.getValue().getÅrsaker(), vurdertMedlemskap.map(VurdertMedlemskap::getBegrunnelse).orElse(null));
            medlemPeriodeDto.setAksjonspunkter(entrySet.getValue().getAksjonspunkter().stream().map(Kodeverdi::getKode).collect(Collectors.toSet()));
            dtoPerioder.add(medlemPeriodeDto);
        }
    }

    private List<OppholdstillatelseDto> mapOppholdstillatelser(Long behandlingId) {
        return personopplysningTjeneste.hentOppholdstillatelser(behandlingId).stream()
            .map(this::mapOppholdstillatelse)
            .collect(Collectors.toList());
    }

    private OppholdstillatelseDto mapOppholdstillatelse(OppholdstillatelseEntitet oppholdstillatelse) {
        var dto = new OppholdstillatelseDto();
        dto.setOppholdstillatelseType(oppholdstillatelse.getTillatelse());
        dto.setFom(oppholdstillatelse.getPeriode().getFomDato().isBefore(OPPHOLD_CUTOFF) ? null : oppholdstillatelse.getPeriode().getFomDato());
        dto.setTom(oppholdstillatelse.getPeriode().getTomDato());
        return dto;
    }

    private Optional<VurdertMedlemskap> finnVurderMedlemskap(Set<VurdertLøpendeMedlemskapEntitet> perioder, Map.Entry<LocalDate, VurderMedlemskap> entrySet) {
        return perioder.stream()
            .filter(it -> it.getVurderingsdato().equals(entrySet.getKey())).map(it -> (VurdertMedlemskap) it).findAny();
    }

    private void mapSkjæringstidspunkt(MedlemV2Dto dto, MedlemskapAggregat aggregat, Set<Aksjonspunkt> aksjonspunkter, BehandlingReferanse ref) {
        final Optional<MedlemskapAggregat> aggregatOpts = Optional.ofNullable(aggregat);
        final Optional<VurdertMedlemskap> vurdertMedlemskapOpt = aggregatOpts.flatMap(MedlemskapAggregat::getVurdertMedlemskap);
        final Set<MedlemPeriodeDto> periodeSet = new HashSet<>();
        LocalDate vurderingsdato = ref.getSkjæringstidspunkt().getSkjæringstidspunktHvisUtledet().orElse(null);
        var begrunnelse = vurdertMedlemskapOpt.map(VurdertMedlemskap::getBegrunnelse).orElseGet(() -> hentBegrunnelseFraAksjonspuntk(aksjonspunkter));
        final MedlemPeriodeDto periodeDto = mapTilPeriodeDto(ref.getBehandlingId(), vurdertMedlemskapOpt, vurderingsdato, Set.of(VurderingsÅrsak.SKJÆRINGSTIDSPUNKT), begrunnelse);
        periodeDto.setAksjonspunkter(aksjonspunkter.stream()
            .map(Aksjonspunkt::getAksjonspunktDefinisjon)
            .filter(MEDL_AKSJONSPUNKTER::contains)
            .map(Kodeverdi::getKode).collect(Collectors.toSet()));
        periodeSet.add(periodeDto);
        dto.setPerioder(periodeSet);
    }

    private MedlemPeriodeDto mapTilPeriodeDto(Long behandlingId, Optional<VurdertMedlemskap> vurdertMedlemskapOpt,
                                              LocalDate vurderingsdato, Set<VurderingsÅrsak> årsaker, String begrunnelse) {
        final MedlemPeriodeDto periodeDto = new MedlemPeriodeDto();
        periodeDto.setÅrsaker(årsaker);
        personopplysningDtoTjeneste.lagPersonopplysningMedlemskapDto(behandlingId, vurderingsdato).ifPresent(periodeDto::setPersonopplysningBruker);
        personopplysningDtoTjeneste.lagAnnenpartPersonopplysningMedlemskapDto(behandlingId, vurderingsdato).ifPresent(periodeDto::setPersonopplysningAnnenPart);
        periodeDto.setVurderingsdato(vurderingsdato);
        periodeDto.setBegrunnelse(begrunnelse);

        if (vurdertMedlemskapOpt.isPresent()) {
            final VurdertMedlemskap vurdertMedlemskap = vurdertMedlemskapOpt.get();
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
        return aksjonspunkter.stream().filter(a -> VilkårType.MEDLEMSKAPSVILKÅRET.equals(a.getAksjonspunktDefinisjon().getVilkårType())).findFirst().map(Aksjonspunkt::getBegrunnelse).orElse(null);
    }
}
