package no.nav.foreldrepenger.web.app.tjenester.behandling.arbeidsforhold;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvangerskapspengerRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingerEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.AlleInntektsmeldingerDtoMapper;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.IAYYtelseDto;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.IayYtelseDtoMapper;
import no.nav.foreldrepenger.domene.arbeidsforhold.dto.InntektsmeldingerDto;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktørArbeid;
import no.nav.foreldrepenger.domene.iay.modell.AktørInntekt;
import no.nav.foreldrepenger.domene.iay.modell.AktørYtelse;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdReferanse;
import no.nav.foreldrepenger.domene.iay.modell.Inntekt;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.iay.modell.OppgittArbeidsforhold;
import no.nav.foreldrepenger.domene.iay.modell.OppgittEgenNæring;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjening;
import no.nav.foreldrepenger.domene.iay.modell.OppgittUtenlandskVirksomhet;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.opptjening.aksjonspunkt.MapYrkesaktivitetTilOpptjeningsperiodeTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.ytelsefordeling.YtelseFordelingTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Path(InntektArbeidYtelseRestTjeneste.BASE_PATH)
@Transactional
public class InntektArbeidYtelseRestTjeneste {

    static final String BASE_PATH = "/behandling";
    private static final String INNTEKT_ARBEID_YTELSE_PART_PATH = "/inntekt-arbeid-ytelse";
    public static final String INNTEKT_ARBEID_YTELSE_PATH = BASE_PATH + INNTEKT_ARBEID_YTELSE_PART_PATH;

    private static final String ALLE_INNTEKTSMELDINGER_PART_PATH = "/inntektsmeldinger-alle";
    public static final String ALLE_INNTEKTSMELDINGER_PATH = BASE_PATH + ALLE_INNTEKTSMELDINGER_PART_PATH;

    private static final String ARBEIDSGIVERE_OPPLYSNINGER_PART_PATH = "/arbeidsgivere-opplysninger";
    public static final String ARBEIDSGIVERE_OPPLYSNINGER_PATH = BASE_PATH + ARBEIDSGIVERE_OPPLYSNINGER_PART_PATH;

    private BehandlingRepository behandlingRepository;
    private IayYtelseDtoMapper ytelseMapper;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    private YtelseFordelingTjeneste ytelseFordelingTjeneste;
    private SvangerskapspengerRepository svangerskapspengerRepository;
    private AlleInntektsmeldingerDtoMapper alleInntektsmeldingerMapper;

    private InntektArbeidYtelseTjeneste iayTjeneste;

    public InntektArbeidYtelseRestTjeneste() {
        // for CDI proxy
    }

    @Inject
    public InntektArbeidYtelseRestTjeneste(BehandlingRepository behandlingRepository,
                                           IayYtelseDtoMapper ytelseMapper,
                                           PersonopplysningTjeneste personopplysningTjeneste,
                                           InntektArbeidYtelseTjeneste iayTjeneste,
                                           ArbeidsgiverTjeneste arbeidsgiverTjeneste,
                                           YtelseFordelingTjeneste ytelseFordelingTjeneste,
                                           SvangerskapspengerRepository svangerskapspengerRepository,
                                           SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                           AlleInntektsmeldingerDtoMapper alleInntektsmeldingerMapper) {
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.iayTjeneste = iayTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.ytelseMapper = ytelseMapper;
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
        this.ytelseFordelingTjeneste = ytelseFordelingTjeneste;
        this.svangerskapspengerRepository = svangerskapspengerRepository;
        this.alleInntektsmeldingerMapper = alleInntektsmeldingerMapper;
    }

    @GET
    @Path(INNTEKT_ARBEID_YTELSE_PART_PATH)
    @Operation(description = "Hent informasjon om innhentet og avklart inntekter, arbeid og ytelser", summary = ("Returnerer info om innhentet og avklart inntekter/arbeid og ytelser for bruker, inkludert hva bruker har vedlagt søknad."), tags = "inntekt-arbeid-ytelse", responses = {
            @ApiResponse(responseCode = "200", description = "Returnerer InntektArbeidYtelseDto, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = IAYYtelseDto.class)))
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public IAYYtelseDto getInntektArbeidYtelser(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
            @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        return getYtelserFraBehandling(behandling);
    }

    private IAYYtelseDto getYtelserFraBehandling(Behandling behandling) {
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());

        if (erSkjæringstidspunktIkkeUtledet(skjæringstidspunkt)) {
            // Tilfelle papirsøknad før registrering
            return new IAYYtelseDto();
        }

        // finn annen part
        var annenPartAktørId = getAnnenPart(behandling.getId());
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        return iayTjeneste.finnGrunnlag(behandling.getId())
            .map(iayg -> ytelseMapper.mapFra(ref, iayg, annenPartAktørId))
            .orElseGet(IAYYtelseDto::new);
    }

    private Optional<AktørId> getAnnenPart(Long behandlingId) {
        return personopplysningTjeneste.hentOppgittAnnenPartAktørId(behandlingId);
    }

    private boolean erSkjæringstidspunktIkkeUtledet(Skjæringstidspunkt skjæringstidspunkt) {
        return skjæringstidspunkt == null || skjæringstidspunkt.getSkjæringstidspunktHvisUtledet().isEmpty();
    }

    @GET
    @Path(ALLE_INNTEKTSMELDINGER_PART_PATH)
    @Operation(description = "Hent informasjon om inntektsmelding", summary = ("Returnerer info om alle inntektsmeldinger."), tags = "inntekt-arbeid-ytelse", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer InntektsmeldingerDto, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = InntektsmeldingerDto.class)))
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public InntektsmeldingerDto getAlleInntektsmeldinger(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
                                                          @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        return iayTjeneste.finnGrunnlag(behandling.getId())
            .map(g -> alleInntektsmeldingerMapper.mapInntektsmeldinger(ref, g))
            .orElseGet(() -> new InntektsmeldingerDto(List.of()));
    }

    @GET
    @Path(ARBEIDSGIVERE_OPPLYSNINGER_PART_PATH)
    @Operation(description = "Hent informasjon om innhentet og avklart inntekter, arbeid og ytelser", summary = ("Returnerer info om innhentet og avklart inntekter/arbeid og ytelser for bruker, inkludert hva bruker har vedlagt søknad."), tags = "inntekt-arbeid-ytelse", responses = {
        @ApiResponse(responseCode = "200", description = "Returnerer InntektArbeidYtelseDto, null hvis ikke eksisterer (GUI støtter ikke NOT_FOUND p.t.)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ArbeidsgiverOversiktDto.class)))
    })
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK)
    public ArbeidsgiverOversiktDto getArbeidsgiverOpplysninger(@TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class)
        @NotNull @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());

        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());

        if (erSkjæringstidspunktIkkeUtledet(skjæringstidspunkt)) {
            return new ArbeidsgiverOversiktDto();
        }

        Set<Arbeidsgiver> arbeidsgivere = new HashSet<>();
        Set<ArbeidsgiverOpplysningerDto> alleReferanser = new HashSet<>();
        List<String> overstyrtNavn = new ArrayList<>();

        if (FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType())) {
            ytelseFordelingTjeneste.hentAggregatHvisEksisterer(behandling.getId())
                .map(YtelseFordelingAggregat::getOppgittFordeling).map(OppgittFordelingEntitet::getPerioder).orElse(Collections.emptyList()).stream()
                .map(OppgittPeriodeEntitet::getArbeidsgiver).filter(Objects::nonNull).forEach(arbeidsgivere::add);
        }
        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(behandling.getFagsakYtelseType())) {
            svangerskapspengerRepository.hentGrunnlag(behandling.getId())
                .map(SvpGrunnlagEntitet::getOpprinneligeTilrettelegginger).map(SvpTilretteleggingerEntitet::getTilretteleggingListe).orElse(Collections.emptyList()).stream()
                .map(SvpTilretteleggingEntitet::getArbeidsgiver).flatMap(Optional::stream).forEach(arbeidsgivere::add);
        }

        iayTjeneste.finnGrunnlag(behandling.getId()).ifPresent(iayg -> {
            iayg.getAktørArbeidFraRegister(behandling.getAktørId()).map(AktørArbeid::hentAlleYrkesaktiviteter).orElse(Collections.emptyList()).stream()
                .map(Yrkesaktivitet::getArbeidsgiver).filter(Objects::nonNull).forEach(arbeidsgivere::add);
            iayg.getBekreftetAnnenOpptjening(behandling.getAktørId()).map(AktørArbeid::hentAlleYrkesaktiviteter).orElse(Collections.emptyList()).stream()
                .map(Yrkesaktivitet::getArbeidsgiver).filter(Objects::nonNull).forEach(arbeidsgivere::add);
            iayg.getAktørInntektFraRegister(behandling.getAktørId()).map(AktørInntekt::getInntekt).orElse(Collections.emptyList()).stream()
                .map(Inntekt::getArbeidsgiver).filter(Objects::nonNull).forEach(arbeidsgivere::add);
            iayg.getAktørYtelseFraRegister(behandling.getAktørId()).map(AktørYtelse::getAlleYtelser).orElse(Collections.emptyList()).stream()
                .flatMap(y -> y.getYtelseGrunnlag().stream()).flatMap(g -> g.getYtelseStørrelse().stream())
                .flatMap(s -> s.getVirksomhet().stream().map(Arbeidsgiver::virksomhet)).forEach(arbeidsgivere::add);
            iayg.getInntektsmeldinger().map(InntektsmeldingAggregat::getAlleInntektsmeldinger).orElse(Collections.emptyList()).stream()
                .map(Inntektsmelding::getArbeidsgiver).filter(Objects::nonNull).forEach(arbeidsgivere::add);
            iayg.getArbeidsforholdInformasjon().map(ArbeidsforholdInformasjon::getArbeidsforholdReferanser).orElse(Collections.emptyList()).stream()
                .map(ArbeidsforholdReferanse::getArbeidsgiver).filter(Objects::nonNull).forEach(arbeidsgivere::add);
            iayg.getArbeidsforholdOverstyringer().stream()
                .map(ArbeidsforholdOverstyring::getArbeidsgiver).filter(Objects::nonNull).forEach(arbeidsgivere::add);
            iayg.getArbeidsforholdOverstyringer().stream()
                .filter(o -> o.getArbeidsgiver() != null && o.getArbeidsgiverNavn() != null && OrgNummer.KUNSTIG_ORG.equals(o.getArbeidsgiver().getIdentifikator()))
                .forEach(o -> overstyrtNavn.add(o.getArbeidsgiverNavn()));
            iayg.getOppgittOpptjening().map(OppgittOpptjening::getEgenNæring).orElse(Collections.emptyList()).stream()
                .map(OppgittEgenNæring::getVirksomhetOrgnr).filter(Objects::nonNull).map(Arbeidsgiver::virksomhet).forEach(arbeidsgivere::add);
            iayg.getOppgittOpptjening().map(OppgittOpptjening::getEgenNæring).orElse(Collections.emptyList()).stream()
                .map(OppgittEgenNæring::getVirksomhet).map(InntektArbeidYtelseRestTjeneste::mapUtlandskOrganisasjon)
                .filter(Objects::nonNull).forEach(alleReferanser::add);
            iayg.getOppgittOpptjening().map(OppgittOpptjening::getOppgittArbeidsforhold).orElse(Collections.emptyList()).stream()
                .map(OppgittArbeidsforhold::getUtenlandskVirksomhet).map(InntektArbeidYtelseRestTjeneste::mapUtlandskOrganisasjon)
                .filter(Objects::nonNull).forEach(alleReferanser::add);
        });

        if (!overstyrtNavn.isEmpty()) {
            alleReferanser.add(new ArbeidsgiverOpplysningerDto(OrgNummer.KUNSTIG_ORG, overstyrtNavn.get(0)));
        } else if (arbeidsgivere.stream().map(Arbeidsgiver::getIdentifikator).anyMatch(OrgNummer.KUNSTIG_ORG::equals)) {
            alleReferanser.add(new ArbeidsgiverOpplysningerDto(OrgNummer.KUNSTIG_ORG, "Lagt til av saksbehandler"));
        }
        var arbeidsgivereDtos = arbeidsgivere.stream()
            .filter(a -> !OrgNummer.KUNSTIG_ORG.equals(a.getIdentifikator()))
            .map(this::mapFra)
            .collect(Collectors.toSet());
        alleReferanser.addAll(arbeidsgivereDtos);
        var oversikt = alleReferanser.stream()
            .collect(Collectors.toMap(ArbeidsgiverOpplysningerDto::getReferanse, Function.identity()));
        // Sørg for at kunstig er tilstede i tilfelle det legges til
        oversikt.putIfAbsent(OrgNummer.KUNSTIG_ORG, new ArbeidsgiverOpplysningerDto(OrgNummer.KUNSTIG_ORG, "Lagt til av saksbehandler"));

        return new ArbeidsgiverOversiktDto(oversikt);
    }


    private ArbeidsgiverOpplysningerDto mapFra(Arbeidsgiver arbeidsgiver) {
        try {
            var opplysninger = arbeidsgiverTjeneste.hent(arbeidsgiver);
            if (arbeidsgiver.getErVirksomhet()) {
                return new ArbeidsgiverOpplysningerDto(arbeidsgiver.getIdentifikator(), opplysninger.getNavn());
            }
            return new ArbeidsgiverOpplysningerDto(arbeidsgiver.getIdentifikator(), opplysninger.getIdentifikator(), opplysninger.getNavn(), opplysninger.getFødselsdato());
        } catch (Exception e) {
            return new ArbeidsgiverOpplysningerDto(arbeidsgiver.getIdentifikator(), "Feil ved oppslag");
        }
    }

    private static ArbeidsgiverOpplysningerDto mapUtlandskOrganisasjon(OppgittUtenlandskVirksomhet virksomhet) {
        if (virksomhet == null || virksomhet.getNavn() == null) {
            return null;
        }
        var ref = MapYrkesaktivitetTilOpptjeningsperiodeTjeneste.lagReferanseForUtlandskOrganisasjon(virksomhet.getNavn());
        return new ArbeidsgiverOpplysningerDto(ref, virksomhet.getNavn());
    }

}
