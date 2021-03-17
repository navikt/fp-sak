package no.nav.foreldrepenger.økonomistøtte;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregning;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.LegacyESBeregningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingInntrekkEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragslinje150;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder.KodeKlassifik;
import no.nav.foreldrepenger.domene.person.pdl.AktørTjeneste;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Betalingsmottaker;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.KjedeNøkkel;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Periode;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Satsen;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.Ytelse;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.YtelsePeriode;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.samlinger.GruppertYtelse;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.samlinger.OverordnetOppdragKjedeOversikt;
import no.nav.foreldrepenger.økonomistøtte.ny.mapper.EksisterendeOppdragMapper;
import no.nav.foreldrepenger.økonomistøtte.ny.mapper.Input;
import no.nav.foreldrepenger.økonomistøtte.ny.mapper.TilkjentYtelseMapper;

@Dependent
public class OppdragInputTjeneste {

    private static final long DUMMY_PT_SIMULERING_ID = -1L;
    private static final String DEFAULT_ANSVARLIG_SAKSBEHANDLER = "VL";

    private LegacyESBeregningRepository beregningRepository;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private TilbakekrevingRepository tilbakekrevingRepository;
    private AktørTjeneste aktørTjeneste;
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
                                AktørTjeneste aktørTjeneste,
                                ØkonomioppdragRepository økonomioppdragRepository,
                                LegacyESBeregningRepository beregningRepository) {
        this.behandlingRepository = behandlingRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.familieHendelseRepository = familieHendelseRepository;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.aktørTjeneste = aktørTjeneste;
        this.økonomioppdragRepository = økonomioppdragRepository;
        this.beregningRepository = beregningRepository;
    }

    public Input lagInput(long behandlingId) {
        return lagInput(behandlingId, DUMMY_PT_SIMULERING_ID);
    }

    public Input lagInput(long behandlingId, long prosessTaskId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var fagsak = behandling.getFagsak();
        var behandlingVedtak = behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandlingId);

        var tidligereOppdragskontroll = hentTidligereOppdragskontroll(fagsak.getSaksnummer());
        var ytelseType = fagsak.getYtelseType();

        var vedtaksdato = behandlingVedtak.map(BehandlingVedtak::getVedtaksdato).orElse(LocalDate.now());

        var inputBuilder = Input.builder()
            .medBehandlingId(behandlingId)
            .medSaksnummer(fagsak.getSaksnummer())
            .medFagsakYtelseType(ytelseType)
            .medVedtaksdato(vedtaksdato)
            .medAnsvarligSaksbehandler(behandlingVedtak.map(BehandlingVedtak::getAnsvarligSaksbehandler).orElse(finnSaksbehandlerFra(behandling)))
            .medBrukerFnr(hentFnrBruker(behandling))
            .medTidligereOppdrag(mapTidligereOppdrag(tidligereOppdragskontroll))
            .medTilkjentYtelse(ytelseType.equals(FagsakYtelseType.ENGANGSTØNAD)
                ? grupperYtelseEngangsstønad(behandlingId, vedtaksdato, tidligereOppdragskontroll)
                : grupperYtelse(hentTilkjentYtelse(behandlingId), getFamilieYtelseType(behandlingId, fagsak)))
            .medBrukInntrekk(hentBrukInntrekk(behandlingId))
            .medProsessTaskId(prosessTaskId)
            ;
        return inputBuilder.build();
    }

    private GruppertYtelse grupperYtelseEngangsstønad(long behandlingId, final LocalDate vedtaksdato, List<Oppdragskontroll> tidligereOppdragskontroll) {
        var sats = hentSatsFraBehandling(behandlingId);
        if (sats.isEmpty()) {
           return GruppertYtelse.TOM;
        }
        var forrigeOppdrag = finnTidligereUtbetaltOppdrag(tidligereOppdragskontroll);
        var gruppertYtelse = GruppertYtelse.builder()
            .leggTilKjede(
                KjedeNøkkel.lag(gjelderFødsel(behandlingId) ? KodeKlassifik.ES_FØDSEL : KodeKlassifik.ES_ADOPSJON, Betalingsmottaker.BRUKER),
                Ytelse.builder()
                    .leggTilPeriode(lagPeriode(forrigeOppdrag.map(Oppdragslinje150::getDatoVedtakFom).orElse(vedtaksdato), Satsen.engang(sats.get())))
                    .build());
        return gruppertYtelse.build();
    }

    private Optional<Oppdragslinje150> finnTidligereUtbetaltOppdrag(final List<Oppdragskontroll> tidligereOppdragskontroll) {
        return tidligereOppdragskontroll.stream()
            .flatMap(ok -> ok.getOppdrag110Liste().stream())
            .filter(Oppdrag110::erKvitteringMottatt)
            .flatMap(oppdrag110 -> oppdrag110.getOppdragslinje150Liste().stream())
            .sorted()
            .findFirst();
    }

    protected YtelsePeriode lagPeriode(LocalDate referanseDato, Satsen sats) {
        return new YtelsePeriode(Periode.of(referanseDato, referanseDato), sats);
    }

    private Optional<Long> hentSatsFraBehandling(long behandlingId) {
        Optional<LegacyESBeregning> beregning = beregningRepository.getSisteBeregning(behandlingId);
        return beregning.map(LegacyESBeregning::getBeregnetTilkjentYtelse);
    }

    private FamilieYtelseType getFamilieYtelseType(long behandlingId, Fagsak fagsak) {
        return finnFamilieYtelseType(behandlingId, fagsak.getYtelseType());
    }

    private String hentFnrBruker(Behandling behandling) {
        return aktørTjeneste.hentPersonIdentForAktørId(behandling.getAktørId()).orElseThrow().getIdent();
    }

    private List<Oppdragskontroll> hentTidligereOppdragskontroll(Saksnummer saksnummer) {
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

    private BeregningsresultatEntitet hentTilkjentYtelse(Long behandlingId) {
        return beregningsresultatRepository.hentUtbetBeregningsresultat(behandlingId).orElse(null);
    }

    private GruppertYtelse grupperYtelse(BeregningsresultatEntitet beregningsresultat, FamilieYtelseType familieYtelseType) {
        TilkjentYtelseMapper tilkjentYtelseMapper = TilkjentYtelseMapper.lagFor(familieYtelseType);
        return tilkjentYtelseMapper.fordelPåNøkler(beregningsresultat);
    }

    private FamilieYtelseType finnFamilieYtelseType(long behandlingId, FagsakYtelseType fagsakYtelseType) {
        if (FagsakYtelseType.FORELDREPENGER.equals(fagsakYtelseType)) {
            return gjelderFødsel(behandlingId)
                ? FamilieYtelseType.FØDSEL
                : FamilieYtelseType.ADOPSJON;
        } else if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(fagsakYtelseType)) {
            return FamilieYtelseType.SVANGERSKAPSPENGER;
        } else {
            return null;
        }
    }

    private boolean gjelderFødsel(Long behandlingId) {
        return familieHendelseRepository.hentAggregat(behandlingId)
            .getGjeldendeVersjon().getGjelderFødsel();
    }

    private static String finnSaksbehandlerFra(Behandling behandling) {
        if (StringUtils.isNotBlank(behandling.getAnsvarligBeslutter())) {
            return behandling.getAnsvarligBeslutter();
        } else if (StringUtils.isNotBlank(behandling.getAnsvarligSaksbehandler())) {
            return behandling.getAnsvarligSaksbehandler();
        }
        return DEFAULT_ANSVARLIG_SAKSBEHANDLER;
    }
}
