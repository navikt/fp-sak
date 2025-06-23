package no.nav.foreldrepenger.økonomistøtte;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoBasis;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.EngangsstønadBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.EngangsstønadBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingInntrekkEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.ØkonomioppdragRepository;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Betalingsmottaker;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.KjedeNøkkel;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Periode;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Satsen;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.Ytelse;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.YtelsePeriode;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.samlinger.GruppertYtelse;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.domene.samlinger.OverordnetOppdragKjedeOversikt;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper.EksisterendeOppdragMapper;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper.OppdragInput;
import no.nav.foreldrepenger.økonomistøtte.oppdrag.mapper.TilkjentYtelseMapper;

@Dependent
public class OppdragInputTjeneste {

    private static final long DUMMY_PT_SIMULERING_ID = -1L;
    private static final String DEFAULT_ANSVARLIG_SAKSBEHANDLER = "VL";

    private EngangsstønadBeregningRepository beregningRepository;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private TilbakekrevingRepository tilbakekrevingRepository;
    private PersoninfoAdapter personinfoAdapter;
    private ØkonomioppdragRepository økonomioppdragRepository;

    private OppdragInputTjeneste() {
        // for cdi proxy
    }

    @Inject
    public OppdragInputTjeneste(BehandlingRepository behandlingRepository,
                                BeregningsresultatRepository beregningsresultatRepository,
                                BehandlingVedtakRepository behandlingVedtakRepository,
                                FamilieHendelseRepository familieHendelseRepository,
                                TilbakekrevingRepository tilbakekrevingRepository,
                                PersoninfoAdapter personinfoAdapter,
                                ØkonomioppdragRepository økonomioppdragRepository,
                                EngangsstønadBeregningRepository beregningRepository) {
        this.behandlingRepository = behandlingRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.familieHendelseRepository = familieHendelseRepository;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.personinfoAdapter = personinfoAdapter;
        this.økonomioppdragRepository = økonomioppdragRepository;
        this.beregningRepository = beregningRepository;
    }

    public OppdragInput lagSimuleringInput(long behandlingId) {
        return lagOppdragInput(behandlingId, DUMMY_PT_SIMULERING_ID, true);
    }

    public OppdragInput lagOppdragInput(long behandlingId, long prosessTaskId) {
        return lagOppdragInput(behandlingId, prosessTaskId, false);
    }

    private OppdragInput lagOppdragInput(long behandlingId, long prosessTaskId, boolean erSimulering) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var fagsak = behandling.getFagsak();
        var behandlingVedtak = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandlingId);
        var tidligereOppdrag = hentTidligereOppdrag(fagsak.getSaksnummer());
        var ytelseType = fagsak.getYtelseType();

        LocalDate vedtaksdato;
        String ansvarligSaksbehandler;

        if (erSimulering) {
            vedtaksdato = LocalDate.now();
            ansvarligSaksbehandler = finnSaksbehandlerFra(behandling);
        } else {
            vedtaksdato = behandlingVedtak.map(BehandlingVedtak::getVedtaksdato).orElseThrow();
            ansvarligSaksbehandler = behandlingVedtak.map(BehandlingVedtak::getAnsvarligSaksbehandler).orElseThrow();
        }

        var inputBuilder = OppdragInput.builder()
            .medBehandlingId(behandlingId)
            .medSaksnummer(fagsak.getSaksnummer())
            .medFagsakYtelseType(ytelseType)
            .medVedtaksdato(vedtaksdato)
            .medAnsvarligSaksbehandler(ansvarligSaksbehandler)
            .medBrukerFnr(hentFnrBruker(behandling))
            .medTidligereOppdrag(mapTidligereOppdrag(tidligereOppdrag))
            .medProsessTaskId(prosessTaskId);

        if (ytelseType.equals(FagsakYtelseType.ENGANGSTØNAD)) {
            inputBuilder
                .medTilkjentYtelse(grupperYtelseEngangsstønad(behandlingId, vedtaksdato, tidligereOppdrag));
        } else {
            var feriepengerDødsdato = personinfoAdapter.hentBrukerBasisForAktør(behandling.getFagsakYtelseType(), behandling.getAktørId())
                .map(PersoninfoBasis::dødsdato)
                .orElse(null);
            inputBuilder
                .medTilkjentYtelse(grupperYtelse(behandlingId, getFamilieYtelseType(behandlingId, fagsak), feriepengerDødsdato))
                .medBrukInntrekk(hentBrukInntrekk(behandlingId));
        }

        return inputBuilder.build();
    }

    private GruppertYtelse grupperYtelseEngangsstønad(long behandlingId, final LocalDate vedtaksdato, List<Oppdragskontroll> tidligereOppdrag) {
        var sats = hentSatsFraBehandling(behandlingId);
        if (sats.isEmpty()) {
           return GruppertYtelse.TOM;
        }
        var førsteOppdrag = finnFørsteUtbetaling(tidligereOppdrag);
        var gruppertYtelse = GruppertYtelse.builder()
            .leggTilKjede(
                KjedeNøkkel.lag(FamilieYtelseType.FØDSEL.equals(gjelderFødsel(behandlingId)) ? KodeKlassifik.ES_FØDSEL : KodeKlassifik.ES_ADOPSJON, Betalingsmottaker.BRUKER),
                Ytelse.builder()
                    .leggTilPeriode(lagPeriode(førsteOppdrag.map(Oppdragslinje150::getDatoVedtakFom).orElse(vedtaksdato), Satsen.engang(sats.get())))
                    .build());
        return gruppertYtelse.build();
    }

    private Optional<Oppdragslinje150> finnFørsteUtbetaling(final List<Oppdragskontroll> tidligereOppdrag) {
        return tidligereOppdrag.stream()
            .flatMap(ok -> ok.getOppdrag110Liste().stream())
            .filter(OppdragKvitteringTjeneste::harPositivKvittering)
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .min(Comparator.comparing(Oppdragslinje150::getDelytelseId));
    }

    protected YtelsePeriode lagPeriode(LocalDate referanseDato, Satsen sats) {
        return new YtelsePeriode(Periode.of(referanseDato, referanseDato), sats);
    }

    private Optional<Long> hentSatsFraBehandling(long behandlingId) {
        var beregning = beregningRepository.hentEngangsstønadBeregning(behandlingId);
        return beregning.map(EngangsstønadBeregning::getBeregnetTilkjentYtelse);
    }

    private FamilieYtelseType getFamilieYtelseType(long behandlingId, Fagsak fagsak) {
        return finnFamilieYtelseType(behandlingId, fagsak.getYtelseType());
    }

    private String hentFnrBruker(Behandling behandling) {
        return personinfoAdapter.hentFnrForAktør(behandling.getAktørId()).getIdent();
    }

    private List<Oppdragskontroll> hentTidligereOppdrag(Saksnummer saksnummer) {
        return økonomioppdragRepository.finnAlleOppdragForSak(saksnummer);
    }

    private OverordnetOppdragKjedeOversikt mapTidligereOppdrag(List<Oppdragskontroll> tidligereOppdragskontroll) {
        return new OverordnetOppdragKjedeOversikt(EksisterendeOppdragMapper.tilKjeder(tidligereOppdragskontroll));
    }

    private boolean hentBrukInntrekk(Long behandlingId) {
        boolean inntrekkErSkruddAv = tilbakekrevingRepository.hentTilbakekrevingInntrekk(behandlingId)
            .map(TilbakekrevingInntrekkEntitet::isAvslåttInntrekk)
            .orElse(false);
        return !inntrekkErSkruddAv;
    }

    private GruppertYtelse grupperYtelse(Long behandlingId, FamilieYtelseType familieYtelseType, LocalDate feriepengerVedDød) {
        var tilkjentAggregat = beregningsresultatRepository.hentBeregningsresultatAggregat(behandlingId).orElse(null);
        var tilkjentYtelseMapper = TilkjentYtelseMapper.lagFor(familieYtelseType, feriepengerVedDød);
        return tilkjentAggregat == null ? GruppertYtelse.TOM :
            tilkjentYtelseMapper.fordelPåNøkler(tilkjentAggregat);
    }

    private FamilieYtelseType finnFamilieYtelseType(long behandlingId, FagsakYtelseType fagsakYtelseType) {
        if (FagsakYtelseType.FORELDREPENGER.equals(fagsakYtelseType)) {
            return gjelderFødsel(behandlingId);
        }
        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(fagsakYtelseType)) {
            return FamilieYtelseType.SVANGERSKAPSPENGER;
        }
        return null;
    }

    private FamilieYtelseType gjelderFødsel(Long behandlingId) {
        return familieHendelseRepository.hentAggregatHvisEksisterer(behandlingId)
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .filter(FamilieHendelseEntitet::getGjelderAdopsjon)
            .map(fh -> FamilieYtelseType.ADOPSJON).orElse(FamilieYtelseType.FØDSEL);
    }

    private static String finnSaksbehandlerFra(Behandling behandling) {
        if ( behandling.getAnsvarligBeslutter() != null && ! behandling.getAnsvarligBeslutter().isBlank()) {
            return behandling.getAnsvarligBeslutter();
        }
        if (behandling.getAnsvarligSaksbehandler() != null && !behandling.getAnsvarligSaksbehandler().isBlank()) {
            return behandling.getAnsvarligSaksbehandler();
        }
        return DEFAULT_ANSVARLIG_SAKSBEHANDLER;
    }
}
