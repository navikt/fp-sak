package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_LOVLIG_OPPHOLD;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_OM_ER_BOSATT;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon.AVKLAR_OPPHOLDSRETT;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
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
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.Inntektspost;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektspostType;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.medlem.api.EndringsresultatPersonopplysningerForMedlemskap;
import no.nav.foreldrepenger.domene.medlem.api.VurderMedlemskap;
import no.nav.foreldrepenger.domene.medlem.api.VurderingsÅrsak;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning.PersonopplysningDtoTjeneste;

@ApplicationScoped
public class MedlemDtoTjeneste {
    private static final String UKJENT_NAVN = "UKJENT NAVN";
    private static final List<AksjonspunktDefinisjon> MEDL_AKSJONSPUNKTER = List.of(AVKLAR_OM_ER_BOSATT,
        AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE,
        AVKLAR_LOVLIG_OPPHOLD,
        AVKLAR_OPPHOLDSRETT);

    private MedlemskapRepository medlemskapRepository;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private BehandlingRepository behandlingRepository;
    private MedlemTjeneste medlemTjeneste;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private PersonopplysningDtoTjeneste personopplysningDtoTjeneste;

    @Inject
    public MedlemDtoTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                 ArbeidsgiverTjeneste arbeidsgiverTjeneste,
                                 SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                 InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                 MedlemTjeneste medlemTjeneste,
                                 PersonopplysningTjeneste personopplysningTjeneste,
                                 PersonopplysningDtoTjeneste personopplysningDtoTjeneste) {

        this.medlemskapRepository = behandlingRepositoryProvider.getMedlemskapRepository();
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
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

    private void mapInntekt(Collection<InntektDto> inntektDto, final String navn, Arbeidsgiver arbeidsgiver, Collection<Inntektspost> inntektsposter) {
        String utbetaler = finnUtbetalerVisningstekst(arbeidsgiver);
        inntektsposter
            .forEach(inntektspost -> {
                InntektDto dto = new InntektDto(); // NOSONAR
                dto.setNavn(navn);
                if (utbetaler != null) {
                    dto.setUtbetaler(utbetaler);
                } else {
                    if (inntektspost.getYtelseType() != null) {
                        dto.setUtbetaler(inntektspost.getYtelseType().getNavn());
                    }
                }

                dto.setFom(inntektspost.getPeriode().getFomDato());
                dto.setTom(inntektspost.getPeriode().getTomDato());
                dto.setYtelse(inntektspost.getInntektspostType().equals(InntektspostType.YTELSE));
                dto.setBelop(inntektspost.getBeløp().getVerdi().intValue());
                inntektDto.add(dto);
            });
    }

    private String finnUtbetalerVisningstekst(Arbeidsgiver arbeidsgiver) {
        if (arbeidsgiver == null) {
            return null;
        }
        if (arbeidsgiver.erAktørId()) {
            return lagPrivatpersontekst(arbeidsgiver);
        } else {
            return arbeidsgiver.getIdentifikator();
        }
    }

    public Optional<MedlemDto> lagMedlemDto(Long behandlingId) {
        Optional<MedlemskapAggregat> medlemskapOpt = medlemskapRepository.hentMedlemskap(behandlingId);
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()));
        Optional<PersonopplysningerAggregat> aggregatOptional = personopplysningTjeneste.hentPersonopplysningerHvisEksisterer(ref);

        MedlemDto dto = new MedlemDto();
        inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingId).ifPresent(aggregat -> dto.setInntekt(lagInntektDto(aggregat, aggregatOptional.orElse(null), ref)));
        settFelterFraMedlemskap(dto, medlemskapOpt, ref);
        settFelterFraPersonopplysninger(dto, aggregatOptional, behandling);

        return Optional.of(dto);
    }

    private void settFelterFraMedlemskap(MedlemDto dto, Optional<MedlemskapAggregat> medlemskapOpt, BehandlingReferanse ref) {
        if (medlemskapOpt.isPresent()) {
            MedlemskapAggregat aggregat = medlemskapOpt.get();
            dto.setMedlemskapPerioder(lagMedlemskapPerioderDto(aggregat.getRegistrertMedlemskapPerioder()));
            Optional<VurdertMedlemskap> vurdertMedlemskapOpt = aggregat.getVurdertMedlemskap();

            if (vurdertMedlemskapOpt.isPresent()) {
                VurdertMedlemskap vurdertMedlemskap = vurdertMedlemskapOpt.get();
                dto.setOppholdsrettVurdering(vurdertMedlemskap.getOppholdsrettVurdering());
                dto.setLovligOppholdVurdering(vurdertMedlemskap.getLovligOppholdVurdering());
                dto.setBosattVurdering(vurdertMedlemskap.getBosattVurdering());
                dto.setMedlemskapManuellVurderingType(vurdertMedlemskap.getMedlemsperiodeManuellVurdering());
                dto.setErEosBorger(vurdertMedlemskap.getErEøsBorger());

                LocalDate stp = ref.getSkjæringstidspunkt().getSkjæringstidspunktHvisUtledet().orElse(null);
                dto.setFom(stp);
            }
        }
    }

    private void settFelterFraPersonopplysninger(MedlemDto dto, Optional<PersonopplysningerAggregat> aggregatOptional, Behandling behandling) {
        // TODO Diamant (Denne gjelder kun revurdering og foreldrepenger, bør skilles ut som egen DTO for FP+BT-004)
        if (aggregatOptional.isPresent()) {
            EndringsresultatPersonopplysningerForMedlemskap endringerIPersonopplysninger = medlemTjeneste.søkerHarEndringerIPersonopplysninger(behandling);
            List<EndringsresultatPersonopplysningerForMedlemskap.Endring> endredeAttributter = endringerIPersonopplysninger.getEndredeAttributter();
            if (!endredeAttributter.isEmpty()) {
                if (!Optional.ofNullable(dto.getFom()).isPresent()) {
                    dto.setFom(endringerIPersonopplysninger.getGjeldendeFra().get());
                }
                List<EndringIPersonopplysningDto> endringer = new ArrayList<>();
                endredeAttributter.forEach(e -> {
                    endringer.add(new EndringIPersonopplysningDto(e));
                });
                dto.setEndringer(endringer);
            } else {
                /**
                 * Ingen endringer i personopplysninger (siden siste vedtatte medlemskapsperiode), så vi setter
                 * gjeldende f.o.m fra nyeste endring i personstatus. Denne vises b.a. ifm. aksjonspunkt 5022
                 */
                if (dto.getFom() != null && aggregatOptional.get().getPersonstatusFor(behandling.getAktørId()) != null) {
                    if (dto.getFom().isBefore(aggregatOptional.get().getPersonstatusFor(behandling.getAktørId()).getPeriode().getFomDato())) {
                        dto.setFom(aggregatOptional.get().getPersonstatusFor(behandling.getAktørId()).getPeriode().getFomDato());
                    }
                }
            }
        }
    }

    public Optional<MedlemV2Dto> lagMedlemPeriodisertDto(Long behandlingId) {
        Optional<MedlemskapAggregat> medlemskapOpt = medlemskapRepository.hentMedlemskap(behandlingId);
        final Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        final MedlemV2Dto dto = new MedlemV2Dto();
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()));
        Optional<PersonopplysningerAggregat> personopplysningerAggregat = personopplysningTjeneste.hentPersonopplysningerHvisEksisterer(ref);
        mapInntekter(dto, behandlingId, personopplysningerAggregat.orElse(null), ref);
        mapSkjæringstidspunkt(dto, medlemskapOpt.orElse(null), behandling.getAksjonspunkter(), ref);
        mapRegistrerteMedlPerioder(dto, medlemskapOpt.map(MedlemskapAggregat::getRegistrertMedlemskapPerioder).orElse(Collections.emptySet()));

        if (behandling.getAksjonspunkter().stream().map(Aksjonspunkt::getAksjonspunktDefinisjon).collect(Collectors.toList()).contains(AksjonspunktDefinisjon.AVKLAR_FORTSATT_MEDLEMSKAP)) {
            mapAndrePerioder(dto, medlemskapOpt.flatMap(MedlemskapAggregat::getVurderingLøpendeMedlemskap).map(VurdertMedlemskapPeriodeEntitet::getPerioder).orElse(Collections.emptySet()), ref);
        }
        return Optional.of(dto);
    }

    private void mapRegistrerteMedlPerioder(MedlemV2Dto dto, Set<MedlemskapPerioderEntitet> perioder) {
        dto.setMedlemskapPerioder(lagMedlemskapPerioderDto(perioder));
    }

    private void mapAndrePerioder(MedlemV2Dto dto, Set<VurdertLøpendeMedlemskapEntitet> perioder, BehandlingReferanse ref) {
        final Map<LocalDate, VurderMedlemskap> vurderingspunkter = medlemTjeneste.utledVurderingspunkterMedAksjonspunkt(ref);
        final Set<MedlemPeriodeDto> dtoPerioder = dto.getPerioder();
        for (Map.Entry<LocalDate, VurderMedlemskap> entrySet : vurderingspunkter.entrySet()) {
            final MedlemPeriodeDto medlemPeriodeDto = mapTilPeriodeDto(ref.getBehandlingId(), finnVurderMedlemskap(perioder, entrySet), entrySet.getKey(), entrySet.getValue().getÅrsaker());
            medlemPeriodeDto.setAksjonspunkter(entrySet.getValue().getAksjonspunkter().stream().map(Kodeverdi::getKode).collect(Collectors.toSet()));
            dtoPerioder.add(medlemPeriodeDto);
        }
    }

    private Optional<VurdertMedlemskap> finnVurderMedlemskap(Set<VurdertLøpendeMedlemskapEntitet> perioder, Map.Entry<LocalDate, VurderMedlemskap> entrySet) {
        return perioder.stream()
            .filter(it -> it.getVurderingsdato().equals(entrySet.getKey())).map(it -> (VurdertMedlemskap) it).findAny();
    }

    private void mapInntekter(MedlemV2Dto dto, Long behandlingId, PersonopplysningerAggregat personopplysningerAggregat, BehandlingReferanse ref) {
        inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingId)
            .ifPresent(aggregat -> dto.setInntekt(lagInntektDto(aggregat, personopplysningerAggregat, ref)));
    }

    private void mapSkjæringstidspunkt(MedlemV2Dto dto, MedlemskapAggregat aggregat, Set<Aksjonspunkt> aksjonspunkter, BehandlingReferanse ref) {
        final Optional<MedlemskapAggregat> aggregatOpts = Optional.ofNullable(aggregat);
        final Optional<VurdertMedlemskap> vurdertMedlemskapOpt = aggregatOpts.flatMap(MedlemskapAggregat::getVurdertMedlemskap);
        final Set<MedlemPeriodeDto> periodeSet = new HashSet<>();
        LocalDate vurderingsdato = ref.getSkjæringstidspunkt().getSkjæringstidspunktHvisUtledet().orElse(null);
        final MedlemPeriodeDto periodeDto = mapTilPeriodeDtoSkjæringstidspunkt(ref.getBehandlingId(), vurdertMedlemskapOpt, vurderingsdato, Set.of(VurderingsÅrsak.SKJÆRINGSTIDSPUNKT), aksjonspunkter);
        periodeDto.setAksjonspunkter(aksjonspunkter.stream()
            .map(Aksjonspunkt::getAksjonspunktDefinisjon)
            .filter(MEDL_AKSJONSPUNKTER::contains)
            .map(Kodeverdi::getKode).collect(Collectors.toSet()));
        periodeSet.add(periodeDto);
        dto.setPerioder(periodeSet);
    }

    private MedlemPeriodeDto mapTilPeriodeDtoSkjæringstidspunkt(Long behandlingId, Optional<VurdertMedlemskap> vurdertMedlemskapOpt, LocalDate vurderingsdato, Set<VurderingsÅrsak> årsaker, Set<Aksjonspunkt> aksjonspunkter) {
        final MedlemPeriodeDto periodeDto = new MedlemPeriodeDto();
        periodeDto.setÅrsaker(årsaker);
        personopplysningDtoTjeneste.lagPersonopplysningDto(behandlingId, vurderingsdato).ifPresent(periodeDto::setPersonopplysninger);
        periodeDto.setVurderingsdato(vurderingsdato);

        if (vurdertMedlemskapOpt.isPresent()) {
            final VurdertMedlemskap vurdertMedlemskap = vurdertMedlemskapOpt.get();
            periodeDto.setBosattVurdering(vurdertMedlemskap.getBosattVurdering());
            periodeDto.setOppholdsrettVurdering(vurdertMedlemskap.getOppholdsrettVurdering());
            periodeDto.setLovligOppholdVurdering(vurdertMedlemskap.getLovligOppholdVurdering());
            periodeDto.setErEosBorger(vurdertMedlemskap.getErEøsBorger());
            periodeDto.setMedlemskapManuellVurderingType(vurdertMedlemskap.getMedlemsperiodeManuellVurdering());
            periodeDto.setBegrunnelse(vurdertMedlemskap.getBegrunnelse() == null ? hentBegrunnelseFraAksjonspuntk(aksjonspunkter) : vurdertMedlemskap.getBegrunnelse());
        }
        return periodeDto;
    }

    private MedlemPeriodeDto mapTilPeriodeDto(Long behandlingId, Optional<VurdertMedlemskap> vurdertMedlemskapOpt, LocalDate vurderingsdato, Set<VurderingsÅrsak> årsaker) {
        final MedlemPeriodeDto periodeDto = new MedlemPeriodeDto();
        periodeDto.setÅrsaker(årsaker);
        personopplysningDtoTjeneste.lagPersonopplysningDto(behandlingId, vurderingsdato).ifPresent(periodeDto::setPersonopplysninger);
        periodeDto.setVurderingsdato(vurderingsdato);

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

    public Optional<MedlemDto> lagMedlemDto(Behandling behandling) {
        return lagMedlemDto(behandling.getId());
    }

    private List<InntektDto> lagInntektDto(InntektArbeidYtelseGrunnlag grunnlag, PersonopplysningerAggregat personopplysningerAggregat, BehandlingReferanse ref) {
        AktørId aktørId = ref.getAktørId();
        List<InntektDto> inntektDto = new ArrayList<>();
        LocalDate stp = ref.getSkjæringstidspunkt().getSkjæringstidspunktHvisUtledet().orElse(null);
        var filter = new InntektFilter(grunnlag.getAktørInntektFraRegister(aktørId)).før(stp).filterPensjonsgivende();
        mapAktørInntekt(inntektDto, aktørId, filter, personopplysningerAggregat);
        return inntektDto;
    }

    private void mapAktørInntekt(List<InntektDto> inntektDto, AktørId aktørId, InntektFilter filter, PersonopplysningerAggregat personopplysningerAggregat) {
        String navn = hentNavnFraTps(aktørId, personopplysningerAggregat);
        filter.forFilter((inntekt, inntektsposter) -> mapInntekt(inntektDto, navn, inntekt.getArbeidsgiver(), inntektsposter));
    }

    private String hentNavnFraTps(AktørId aktørId, PersonopplysningerAggregat personopplysningerAggregat) {
        if (personopplysningerAggregat == null) {
            return UKJENT_NAVN;
        }
        PersonopplysningEntitet personopplysning = personopplysningerAggregat.getAktørPersonopplysningMap().get(aktørId);
        if (personopplysning == null) {
            return UKJENT_NAVN;
        }
        return personopplysning.getNavn(); //$NON-NLS-1$
    }

    private String lagPrivatpersontekst(Arbeidsgiver arbeidsgiver) {
        ArbeidsgiverOpplysninger opplysninger = arbeidsgiverTjeneste.hent(arbeidsgiver);
        if (opplysninger.getNavn() == null) {
            return UKJENT_NAVN;
        }
        String navn = opplysninger.getNavn();
        String avkortetNavn = navn.length() < 5 ? navn : navn.substring(0, 5);
        String formatertFødselsdato = opplysninger.getFødselsdato() != null
            ? opplysninger.getFødselsdato().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            : opplysninger.getIdentifikator();
        return avkortetNavn + "..." + "(" + formatertFødselsdato + ")";
    }

    //TODO(OJR) Hack!!! kan fjernes hvis man ønsker å utføre en migrerning(kompleks) av gamle medlemskapvurdering i produksjon
    private String hentBegrunnelseFraAksjonspuntk(Set<Aksjonspunkt> aksjonspunkter) {
        return aksjonspunkter.stream().filter(a -> VilkårType.MEDLEMSKAPSVILKÅRET.equals(a.getAksjonspunktDefinisjon().getVilkårType())).findFirst().map(Aksjonspunkt::getBegrunnelse).orElse(null);
    }
}
