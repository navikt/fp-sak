package no.nav.foreldrepenger.økonomi.ny.tjeneste;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingInntrekkEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomi.ny.mapper.Input;
import no.nav.foreldrepenger.økonomi.ny.mapper.LagOppdragTjeneste;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragskontrollPostConditionCheck;

@ApplicationScoped
public class NyOppdragskontrollTjeneste {

    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private TilbakekrevingRepository tilbakekrevingRepository;

    private LagOppdragTjeneste lagOppdragTjeneste;

    NyOppdragskontrollTjeneste() {
        //for cdi proxy
    }

    @Inject
    public NyOppdragskontrollTjeneste(BehandlingRepository behandlingRepository, BeregningsresultatRepository beregningsresultatRepository, BehandlingVedtakRepository behandlingVedtakRepository, FamilieHendelseRepository familieHendelseRepository, TilbakekrevingRepository tilbakekrevingRepository, LagOppdragTjeneste lagOppdragTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.familieHendelseRepository = familieHendelseRepository;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.lagOppdragTjeneste = lagOppdragTjeneste;
    }

    /**
     * Brukes ved iverksettelse. Sender over kun nødvendige endringer til oppdragssystemet.
     */
    public Optional<Oppdragskontroll> opprettOppdrag(Long behandlingId, Long prosessTaskId) {
        return opprettOppdrag(behandlingId, prosessTaskId, false);
    }

    /**
     * Brukes ved simulering. Finner tidligste endringstidspunkt på tvers av mottakere, og sender alt for alle mottakere f.o.m. det felles endringstidspunktet.
     * Det gjør at simuleringsvisningen får data for alle mottakere og inntektskategorier, og ikke bare for de som er endret.
     */
    public Optional<Oppdragskontroll> opprettOppdragFraFellesEndringstidspunkt(Long behandlingId, Long prosessTaskId) {
        return opprettOppdrag(behandlingId, prosessTaskId, true);
    }

    public Optional<Oppdragskontroll> opprettOppdrag(Long behandlingId, Long prosessTaskId, boolean brukFellesEndringstidspunkt) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        //BehandlingVedtak vedtak = behandlingVedtakRepository.hentForBehandling(behandlingId);
        BeregningsresultatEntitet tilkjentYtelse = hentTilkjentYtelse(behandlingId);
        boolean brukInntrekk = hentBrukInntrekk(behandlingId);

        Saksnummer saksnummer = behandling.getFagsak().getSaksnummer();

        Input input = Input.builder()
            .medTilkjentYtelse(tilkjentYtelse)
            .medBrukInntrekk(brukInntrekk)
            .medFagsakYtelseType(behandling.getFagsak().getYtelseType())
            .medFamilieYtelseType(finnFamilieYtelseType(behandling))
            .medBruker(behandling.getAktørId())
            .medSaksnummer(saksnummer)
            .medBehandlingId(behandlingId)
          //  .medVedtaksdato(vedtak.getVedtaksdato())
            .medAnsvarligSaksbehandler(behandling.getAnsvarligBeslutter())
            .medProsessTaskId(prosessTaskId)
            .build();

        Oppdragskontroll oppdragskontroll = lagOppdragTjeneste.lagOppdrag(input, brukFellesEndringstidspunkt);
        if (oppdragskontroll != null) {
            OppdragskontrollPostConditionCheck.valider(oppdragskontroll);
            return Optional.of(oppdragskontroll);
        }
        return Optional.empty();
    }

    private boolean hentBrukInntrekk(Long behandlingId) {
        boolean inntrekkErSkruddAv = tilbakekrevingRepository.hentTilbakekrevingInntrekk(behandlingId)
            .map(TilbakekrevingInntrekkEntitet::isAvslåttInntrekk)
            .orElse(false);
        return !inntrekkErSkruddAv;
    }

    BeregningsresultatEntitet hentTilkjentYtelse(Long behandlingId) {
        BehandlingBeregningsresultatEntitet beregningsresultatAggregat = beregningsresultatRepository.hentBeregningsresultatAggregat(behandlingId).orElse(null);
        if (beregningsresultatAggregat == null) {
            return null;
        }
        if (beregningsresultatAggregat.getUtbetBeregningsresultatFP() != null) {
            return beregningsresultatAggregat.getUtbetBeregningsresultatFP();
        }
        return beregningsresultatAggregat.getBgBeregningsresultatFP();
    }

    private FamilieYtelseType finnFamilieYtelseType(Behandling behandling) {
        FagsakYtelseType fagsakYtelseType = behandling.getFagsakYtelseType();
        if (FagsakYtelseType.FORELDREPENGER.equals(fagsakYtelseType)) {
            return gjelderFødsel(behandling.getId())
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
}
