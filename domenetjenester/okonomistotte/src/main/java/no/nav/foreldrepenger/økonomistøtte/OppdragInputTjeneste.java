package no.nav.foreldrepenger.økonomistøtte;

import java.time.LocalDate;
import java.util.List;

import javax.enterprise.context.Dependent;
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
import no.nav.foreldrepenger.domene.person.pdl.AktørTjeneste;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.samlinger.GruppertYtelse;
import no.nav.foreldrepenger.økonomistøtte.ny.domene.samlinger.OverordnetOppdragKjedeOversikt;
import no.nav.foreldrepenger.økonomistøtte.ny.mapper.EksisterendeOppdragMapper;
import no.nav.foreldrepenger.økonomistøtte.ny.mapper.Input;
import no.nav.foreldrepenger.økonomistøtte.ny.mapper.TilkjentYtelseMapper;

@Dependent
public class OppdragInputTjeneste {

    private static final long DUMMY_PT_SIMULERING_ID = -1L;

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
                                ØkonomioppdragRepository økonomioppdragRepository) {
        this.behandlingRepository = behandlingRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.familieHendelseRepository = familieHendelseRepository;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.aktørTjeneste = aktørTjeneste;
        this.økonomioppdragRepository = økonomioppdragRepository;
    }

    public Input lagInput(long behandlingId, long prosessTaskId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        var fagsak = behandling.getFagsak();
        var familieYtelseType = finnFamilieYtelseType(behandlingId, fagsak.getYtelseType());
        var build = Input.builder()
            .medBehandlingId(behandlingId)
            .medSaksnummer(fagsak.getSaksnummer())
            .medFagsakYtelseType(fagsak.getYtelseType())
            .medVedtaksdato(hentVedtaksdato(behandlingId))
            .medAnsvarligSaksbehandler(behandling.getAnsvarligBeslutter())
            //.medBehandlingResultatType()
            .medBrukerFnr(hentFnrBruker(behandling))
            .medFamilieYtelseType(familieYtelseType)
            .medTilkjentYtelse(grupperYtelse(hentTilkjentYtelse(behandlingId), familieYtelseType))
            .medBrukInntrekk(hentBrukInntrekk(behandlingId))
            .medProsessTaskId(prosessTaskId)
            .medTidligereOppdrag(mapTidligereOppdrag(hentTidligereOppdragskontroll(fagsak.getSaksnummer())))
            ;
        return build.build();
    }

    public Input lagInput(long behandlingId) {
        return lagInput(behandlingId, DUMMY_PT_SIMULERING_ID);
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

    private LocalDate hentVedtaksdato(Long behandlingId) {
        return behandlingVedtakRepository.hentForBehandlingHvisEksisterer(behandlingId).map(BehandlingVedtak::getVedtaksdato).orElseThrow();
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

}
