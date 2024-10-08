package no.nav.foreldrepenger.domene.abakus;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.inject.Inject;

import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
import no.nav.abakus.iaygrunnlag.inntektsmelding.v1.InntektsmeldingerDto;
import no.nav.abakus.iaygrunnlag.request.Dataset;
import no.nav.abakus.iaygrunnlag.request.InntektArbeidYtelseGrunnlagRequest;
import no.nav.abakus.iaygrunnlag.request.InntektsmeldingerMottattRequest;
import no.nav.abakus.iaygrunnlag.request.InntektsmeldingerRequest;
import no.nav.abakus.iaygrunnlag.request.KopierGrunnlagRequest;
import no.nav.abakus.iaygrunnlag.request.OppgittOpptjeningMottattRequest;
import no.nav.abakus.iaygrunnlag.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.abakus.iaygrunnlag.v1.OverstyrtInntektArbeidYtelseDto;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.abakus.mapping.IAYFraDtoMapper;
import no.nav.foreldrepenger.domene.abakus.mapping.IAYTilDtoMapper;
import no.nav.foreldrepenger.domene.abakus.mapping.KodeverkMapper;
import no.nav.foreldrepenger.domene.abakus.mapping.MapInntektsmeldinger;
import no.nav.foreldrepenger.domene.arbeidsforhold.IAYDiffsjekker;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregat;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlagBuilder;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.foreldrepenger.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.vedtak.exception.TekniskException;

@ApplicationScoped
@Default
public class AbakusInntektArbeidYtelseTjeneste implements InntektArbeidYtelseTjeneste {

    private AbakusTjeneste abakusTjeneste;
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private IAYRequestCache requestCache;

    /**
     * CDI ctor for proxies.
     */
    AbakusInntektArbeidYtelseTjeneste() {
        // CDI proxy
    }

    /**
     * Standard ctor som injectes av CDI.
     */
    @Inject
    public AbakusInntektArbeidYtelseTjeneste(AbakusTjeneste abakusTjeneste,
            BehandlingRepository behandlingRepository,
            FagsakRepository fagsakRepository,
            IAYRequestCache requestCache) {
        this.behandlingRepository = Objects.requireNonNull(behandlingRepository, "behandlingRepository");
        this.abakusTjeneste = Objects.requireNonNull(abakusTjeneste, "abakusTjeneste");
        this.requestCache = Objects.requireNonNull(requestCache, "requestCache");
        this.fagsakRepository = Objects.requireNonNull(fagsakRepository, "fagsakRepository");
    }

    @Override
    public InntektArbeidYtelseGrunnlag hentGrunnlag(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var grunnlag = hentGrunnlagHvisEksisterer(behandling);
        if (grunnlag == null) {
            throw new IllegalStateException("Fant ikke IAY grunnlag som forventet.");
        }
        return grunnlag;
    }

    @Override
    public InntektArbeidYtelseGrunnlagDto hentGrunnlagKontrakt(Long behandlingId) {
        var request = initRequest(behandlingRepository.hentBehandling(behandlingId));
        return hentGrunnlag(request);
    }

    private InntektArbeidYtelseGrunnlag hentGrunnlagHvisEksisterer(Behandling behandling) {
        var request = initRequest(behandling);
        var aktørId = behandling.getAktørId();
        return hentOgMapGrunnlag(request, aktørId);
    }

    @Override
    public InntektArbeidYtelseGrunnlag hentGrunnlag(UUID behandlingUuid) {
        var behandling = behandlingRepository.hentBehandling(behandlingUuid);
        var grunnlag = hentGrunnlagHvisEksisterer(behandling);
        if (grunnlag == null) {
            throw new IllegalStateException("Fant ikke IAY grunnlag som forventet.");
        }
        return grunnlag;
    }

    @Override
    public Optional<InntektArbeidYtelseGrunnlag> finnGrunnlag(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        return Optional.ofNullable(this.hentGrunnlagHvisEksisterer(behandling));
    }

    /**
     * Anbefalt ikke bruk? Mulig behov for totrinnskontroll som lenker direkte til
     * nøkkel
     */
    @Override
    public InntektArbeidYtelseGrunnlag hentGrunnlagPåId(Long behandlingId, UUID inntektArbeidYtelseGrunnlagUuid) {
        var dto = requestCache.getGrunnlag(inntektArbeidYtelseGrunnlagUuid);
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (dto == null) {
            var request = initRequest(behandling, inntektArbeidYtelseGrunnlagUuid);
            var aktørId = behandling.getAktørId();
            var grunnlaget = hentOgMapGrunnlag(request, aktørId);
            if (grunnlaget == null || grunnlaget.getEksternReferanse() == null || !grunnlaget.getEksternReferanse()
                .equals(inntektArbeidYtelseGrunnlagUuid)) {
                throw new IllegalStateException("Fant ikke grunnlag med referanse=" + inntektArbeidYtelseGrunnlagUuid);
            }
            return grunnlaget;
        }
        return dto;
    }

    private InntektArbeidYtelseGrunnlagRequest initRequest(Behandling behandling, UUID inntektArbeidYtelseGrunnlagUuid) {
        var request = new InntektArbeidYtelseGrunnlagRequest(new AktørIdPersonident(behandling.getAktørId().getId()));
        request.medSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi());
        request.medYtelseType(KodeverkMapper.fraFagsakYtelseType(behandling.getFagsakYtelseType()));
        request.forKobling(behandling.getUuid());
        request.forGrunnlag(inntektArbeidYtelseGrunnlagUuid);
        request.medDataset(Arrays.asList(Dataset.values()));
        return request;
    }

    @Override
    public InntektArbeidYtelseAggregatBuilder opprettBuilderForRegister(Long behandlingId) {
        var iayGrunnlag = finnGrunnlag(behandlingId);
        return opprettBuilderFor(VersjonType.REGISTER, UUID.randomUUID(), LocalDateTime.now(), iayGrunnlag);
    }

    @Override
    public InntektArbeidYtelseAggregatBuilder opprettBuilderForSaksbehandlet(Long behandlingId) {
        var iayGrunnlag = finnGrunnlag(behandlingId);
        return opprettBuilderFor(VersjonType.SAKSBEHANDLET, UUID.randomUUID(), LocalDateTime.now(), iayGrunnlag);
    }

    @Override
    public List<Inntektsmelding> hentUnikeInntektsmeldingerForSak(Saksnummer saksnummer) {
        var fagsakOpt = fagsakRepository.hentSakGittSaksnummer(saksnummer);

        if (fagsakOpt.isPresent()) {
            var fagsak = fagsakOpt.get();
            // Hent grunnlag fra abakus
            return hentOgMapAlleInntektsmeldinger(fagsak);

        }
        return List.of();
    }

    @Override
    public void lagreIayAggregat(Long behandlingId, InntektArbeidYtelseAggregatBuilder inntektArbeidYtelseAggregatBuilder) {
        var iayGrunnlagBuilder = getGrunnlagBuilder(behandlingId, inntektArbeidYtelseAggregatBuilder);
        konverterOgLagreOverstyring(behandlingId, iayGrunnlagBuilder.build());
    }

    @Override
    public void lagreOppgittOpptjening(Long behandlingId, OppgittOpptjeningBuilder oppgittOpptjeningBuilder) {
        if (oppgittOpptjeningBuilder == null) {
            return;
        }
        OppgittOpptjeningMottattRequest request = lagOppgittOpptjeningRequest(oppgittOpptjeningBuilder, behandlingId);
        try {
            abakusTjeneste.lagreOppgittOpptjening(request);
        } catch (IOException e) {
            throw feilVedKallTilAbakus("Lagre oppgitt opptjening i abakus: " + e.getMessage(), e);
        }
    }

    @Override
    public void lagreOverstyrtOppgittOpptjening(Long behandlingId, OppgittOpptjeningBuilder oppgittOpptjeningBuilder) {
        if (oppgittOpptjeningBuilder == null) {
            return;
        }
        OppgittOpptjeningMottattRequest request = lagOppgittOpptjeningRequest(oppgittOpptjeningBuilder, behandlingId);
        try {
            abakusTjeneste.lagreOverstyrtOppgittOpptjening(request);
        } catch (IOException e) {
            throw feilVedKallTilAbakus("Lagre oppgitt opptjening i abakus: " + e.getMessage(), e);
        }

    }

    @Override
    public void lagreOppgittOpptjeningNullstillOverstyring(Long behandlingId, OppgittOpptjeningBuilder oppgittOpptjeningBuilder) {
        if (oppgittOpptjeningBuilder == null) {
            return;
        }
        OppgittOpptjeningMottattRequest request = lagOppgittOpptjeningRequest(oppgittOpptjeningBuilder, behandlingId);
        try {
            abakusTjeneste.lagreOppgittOpptjeningNullstillOverstyring(request);
        } catch (IOException e) {
            throw feilVedKallTilAbakus("Lagre oppgitt opptjening i abakus: " + e.getMessage(), e);
        }

    }

    @Override
    public void lagreOverstyrtArbeidsforhold(Long behandlingId, ArbeidsforholdInformasjonBuilder informasjonBuilder) {
        Objects.requireNonNull(informasjonBuilder, "informasjonBuilder");

        var iayGrunnlagBuilder = opprettGrunnlagBuilderFor(behandlingId);

        iayGrunnlagBuilder.medInformasjon(informasjonBuilder.build());

        konverterOgLagreOverstyring(behandlingId, iayGrunnlagBuilder.build());
    }

    @Override
    public List<Inntektsmelding> lagreInntektsmeldinger(Saksnummer saksnummer, Long behandlingId,
                                                        Collection<InntektsmeldingBuilder> inntektsmeldingBuilderCollection) {
        Objects.requireNonNull(inntektsmeldingBuilderCollection, "inntektsmeldingBuilderCollection");
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var inntektsmeldingerDto = new IAYTilDtoMapper(behandling.getAktørId(),
                KodeverkMapper.fraFagsakYtelseType(behandling.getFagsakYtelseType()),
                null, behandling.getUuid()).mapTilDto(inntektsmeldingBuilderCollection);

        if (inntektsmeldingerDto == null) {
            return Collections.emptyList();
        }
        var aktør = new AktørIdPersonident(behandling.getAktørId().getId());
        var inntektsmeldingerMottattRequest = new InntektsmeldingerMottattRequest(saksnummer.getVerdi(),
                behandling.getUuid(), aktør,
                KodeverkMapper.fraFagsakYtelseType(behandling.getFagsakYtelseType()), inntektsmeldingerDto);
        try {
            abakusTjeneste.lagreInntektsmeldinger(inntektsmeldingerMottattRequest);
        } catch (IOException e) {
            throw feilVedKallTilAbakus("Lagre mottatte inntektsmeldinger i abakus: " + e.getMessage(), e);
        }
        //Brukes ikke til noe
        return Collections.emptyList();
    }

    @Override
    public void fjernSaksbehandletVersjon(Long behandlingId) {
        var iayGrunnlagOpt = finnGrunnlag(behandlingId);
        if (iayGrunnlagOpt.isPresent()) {
            var iayGrunnlag = iayGrunnlagOpt.get();

            if (iayGrunnlag.getSaksbehandletVersjon().isPresent()) {
                iayGrunnlag.fjernSaksbehandlet();
                lagreOverstyrtGrunnlag(iayGrunnlag, behandlingId);
            }
        }
    }

    @Override
    public void kopierGrunnlagFraEksisterendeBehandling(Long fraBehandlingId, Long tilBehandlingId) {
        var fraBehandling = behandlingRepository.hentBehandling(fraBehandlingId);
        var tilBehandling = behandlingRepository.hentBehandling(tilBehandlingId);
        var request = new KopierGrunnlagRequest(tilBehandling.getFagsak().getSaksnummer().getVerdi(),
                tilBehandling.getUuid(),
                fraBehandling.getUuid(),
                KodeverkMapper.fraFagsakYtelseType(tilBehandling.getFagsakYtelseType()),
                new AktørIdPersonident(tilBehandling.getAktørId().getId()),
                Set.of(Dataset.values()));
        try {
            // Kopier grunnlag tar med nye inntektsmeldinger som kan ha kommet etter forrige grunnlag.
            // Berørte, feriepenger og utsatt start behandlinger skal kopiere nyeste inntektsmeldigner
            if (SpesialBehandling.erSpesialBehandling(tilBehandling)) {
                abakusTjeneste.kopierGrunnlagUtenNyeInntektsmeldinger(request);
            } else {
                abakusTjeneste.kopierGrunnlag(request);
            }
        } catch (IOException e) {
            throw feilVedKallTilAbakus("Lagre mottatte inntektsmeldinger i abakus: " + e.getMessage(), e);
        }
    }

    @Override
    public void kopierGrunnlagFraEksisterendeBehandlingUtenVurderinger(Long fraBehandlingId, Long tilBehandlingId) {
        var fraBehandling = behandlingRepository.hentBehandling(fraBehandlingId);
        var tilBehandling = behandlingRepository.hentBehandling(tilBehandlingId);
        var request = new KopierGrunnlagRequest(tilBehandling.getFagsak().getSaksnummer().getVerdi(),
            tilBehandling.getUuid(),
            fraBehandling.getUuid(),
            KodeverkMapper.fraFagsakYtelseType(tilBehandling.getFagsakYtelseType()),
            new AktørIdPersonident(tilBehandling.getAktørId().getId()),
            Set.of(Dataset.INNTEKTSMELDING, Dataset.OPPGITT_OPPTJENING, Dataset.OVERSTYRT_OPPGITT_OPPTJENING));
        try {
            abakusTjeneste.kopierGrunnlag(request);
        } catch (IOException e) {
            throw feilVedKallTilAbakus("Lagre mottatte inntektsmeldinger i abakus: " + e.getMessage(), e);
        }
    }

    private OppgittOpptjeningMottattRequest lagOppgittOpptjeningRequest(OppgittOpptjeningBuilder oppgittOpptjeningBuilder,
                                                                        Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var aktør = new AktørIdPersonident(behandling.getAktørId().getId());
        var oppgittOpptjening = new IAYTilDtoMapper(behandling.getAktørId(),
            KodeverkMapper.fraFagsakYtelseType(behandling.getFagsakYtelseType()),
            null, behandling.getUuid()).mapTilDto(oppgittOpptjeningBuilder);
        return new OppgittOpptjeningMottattRequest(behandling.getFagsak().getSaksnummer().getVerdi(), behandling.getUuid(), aktør,
            KodeverkMapper.fraFagsakYtelseType(behandling.getFagsakYtelseType()), oppgittOpptjening);
    }

    private InntektArbeidYtelseGrunnlag hentOgMapGrunnlag(InntektArbeidYtelseGrunnlagRequest request, AktørId aktørId) {
        var dto = hentGrunnlag(request);
        var forespurtGrunnlagsRef = request.getGrunnlagReferanse() != null ? request.getGrunnlagReferanse()
                : request.getSisteKjenteGrunnlagReferanse();
        var sisteGrunnlag = requestCache.getGrunnlag(forespurtGrunnlagsRef);
        if (dto == null && sisteGrunnlag == null) {
            return null;
        }
        if (dto == null) {
            return sisteGrunnlag;
        }
        return mapOgCacheGrunnlag(dto, aktørId, request.getGrunnlagVersjon() == InntektArbeidYtelseGrunnlagRequest.GrunnlagVersjon.SISTE);
    }

    private InntektArbeidYtelseGrunnlag mapOgCacheGrunnlag(InntektArbeidYtelseGrunnlagDto grunnlagDto, AktørId aktørId, boolean isAktiv) {
        var grunnlag = mapResult(aktørId, grunnlagDto, isAktiv);
        requestCache.leggTil(grunnlag);
        return grunnlag;
    }

    private InntektsmeldingerDto hentUnikeInntektsmeldinger(InntektsmeldingerRequest request) {
        try {
            return abakusTjeneste.hentUnikeUnntektsmeldinger(request);
        } catch (IOException e) {
            throw feilVedKallTilAbakus("Kunne ikke hente inntektsmeldinger fra Abakus: " + e.getMessage(), e);
        }
    }

    private InntektArbeidYtelseGrunnlagDto hentGrunnlag(InntektArbeidYtelseGrunnlagRequest request) {
        try {
            return abakusTjeneste.hentGrunnlag(request);
        } catch (IOException e) {
            throw feilVedKallTilAbakus("Kunne ikke hente grunnlag fra Abakus: " + e.getMessage(), e);
        }
    }

    private InntektsmeldingAggregat mapResult(InntektsmeldingerDto dto) {
        var mapInntektsmeldinger = new MapInntektsmeldinger.MapFraDto();
        var dummyBuilder = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());
        return mapInntektsmeldinger.map(dummyBuilder, dto);
    }

    private InntektArbeidYtelseGrunnlag mapResult(AktørId aktørId, InntektArbeidYtelseGrunnlagDto dto, boolean isAktiv) {
        var inntektArbeidYtelseGrunnlag = new IAYFraDtoMapper(aktørId).mapTilGrunnlagInklusivRegisterdata(dto, isAktiv);
        return new AbakusInntektArbeidYtelseGrunnlag(inntektArbeidYtelseGrunnlag, dto.getKoblingReferanse());
    }

    private InntektArbeidYtelseGrunnlagRequest initRequest(Behandling behandling) {
        var request = new InntektArbeidYtelseGrunnlagRequest(new AktørIdPersonident(behandling.getAktørId().getId()));
        request.medSisteKjenteGrunnlagReferanse(requestCache.getSisteAktiveGrunnlagReferanse(behandling.getUuid()));
        request.medSaksnummer(behandling.getFagsak().getSaksnummer().getVerdi());
        request.medYtelseType(KodeverkMapper.fraFagsakYtelseType(behandling.getFagsakYtelseType()));
        request.forKobling(behandling.getUuid());
        request.medDataset(Arrays.asList(Dataset.values()));
        return request;
    }

    private InntektsmeldingerRequest initInntektsmeldingerRequest(Fagsak fagsak) {
        var request = new InntektsmeldingerRequest(new AktørIdPersonident(fagsak.getAktørId().getId()));
        request.setSaksnummer(fagsak.getSaksnummer().getVerdi());
        request.setYtelseType(KodeverkMapper.fraFagsakYtelseType(fagsak.getYtelseType()));
        return request;
    }

    private List<Inntektsmelding> hentOgMapAlleInntektsmeldinger(Fagsak fagsak) {
        var request = initInntektsmeldingerRequest(fagsak);
        var dto = hentUnikeInntektsmeldinger(request);
        return mapResult(dto).getAlleInntektsmeldinger();
    }

    private InntektArbeidYtelseAggregatBuilder opprettBuilderFor(VersjonType versjonType, UUID angittReferanse, LocalDateTime opprettetTidspunkt,
            Optional<InntektArbeidYtelseGrunnlag> grunnlag) {
        var grunnlagBuilder = InntektArbeidYtelseGrunnlagBuilder.oppdatere(grunnlag);
        Objects.requireNonNull(grunnlagBuilder, "grunnlagBuilder");
        var aggregat = Optional.ofNullable(grunnlagBuilder.getKladd());
        Objects.requireNonNull(aggregat, "aggregat");
        if (aggregat.isPresent()) {
            var aggregat1 = aggregat.get();
            return InntektArbeidYtelseAggregatBuilder.builderFor(hentRiktigVersjon(versjonType, aggregat1), angittReferanse, opprettetTidspunkt,
                    versjonType);
        }
        throw new IllegalArgumentException("aggregat kan ikke være null: " + angittReferanse);
    }

    private Optional<InntektArbeidYtelseAggregat> hentRiktigVersjon(VersjonType versjonType, InntektArbeidYtelseGrunnlag ytelseGrunnlag) {
        if (versjonType == VersjonType.REGISTER) {
            return ytelseGrunnlag.getRegisterVersjon();
        }
        if (versjonType == VersjonType.SAKSBEHANDLET) {
            return ytelseGrunnlag.getSaksbehandletVersjon();
        }
        throw new IllegalStateException("Kunne ikke finne riktig versjon av InntektArbeidYtelseAggregat");
    }

    private InntektArbeidYtelseGrunnlagBuilder getGrunnlagBuilder(Long behandlingId, InntektArbeidYtelseAggregatBuilder iayAggregetBuilder) {
        Objects.requireNonNull(iayAggregetBuilder, "iayAggregetBuilder");
        var opptjeningAggregatBuilder = opprettGrunnlagBuilderFor(behandlingId);
        opptjeningAggregatBuilder.medData(iayAggregetBuilder);
        return opptjeningAggregatBuilder;
    }

    private InntektArbeidYtelseGrunnlagBuilder opprettGrunnlagBuilderFor(Long behandlingId) {
        var inntektArbeidGrunnlag = finnGrunnlag(behandlingId);
        return InntektArbeidYtelseGrunnlagBuilder.oppdatere(inntektArbeidGrunnlag);
    }

    private void konverterOgLagreOverstyring(Long behandlingId, InntektArbeidYtelseGrunnlag nyttGrunnlag) {
        Objects.requireNonNull(behandlingId, "behandlingId");
        if (nyttGrunnlag == null) {
            return;
        }

        var tidligereAggregat = finnGrunnlag(behandlingId);
        if (tidligereAggregat.isPresent()) {
            var tidligereGrunnlag = tidligereAggregat.get();
            if (new IAYDiffsjekker(false).getDiffEntity().diff(tidligereGrunnlag, nyttGrunnlag).isEmpty()) {
                return;
            }
            lagreOverstyrtGrunnlag(nyttGrunnlag, behandlingId);
        } else {
            lagreOverstyrtGrunnlag(nyttGrunnlag, behandlingId);
        }
    }

    private void lagreOverstyrtGrunnlag(InntektArbeidYtelseGrunnlag nyttGrunnlag, Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        try {
            abakusTjeneste.lagreOverstyrtGrunnlag(konverterTilOverstyringDto(behandling, nyttGrunnlag));
        } catch (IOException e) {
            throw feilVedKallTilAbakus("Kunne ikke lagre overstyrt grunnlag i Abakus: " + e.getMessage(), e);
        }
    }

    private OverstyrtInntektArbeidYtelseDto konverterTilOverstyringDto(Behandling behandling, InntektArbeidYtelseGrunnlag gr) {
        var tilDto = new IAYTilDtoMapper(behandling.getAktørId(), KodeverkMapper.fraFagsakYtelseType(behandling.getFagsakYtelseType()),
            gr.getEksternReferanse(), behandling.getUuid());
        return tilDto.mapTilOverstyringDto(gr);
    }

    private static TekniskException feilVedKallTilAbakus(String feilmelding, Throwable t) {
        return new TekniskException("FP-118669", String.format("Feil ved kall til Abakus: %s", feilmelding), t);
    }

}
