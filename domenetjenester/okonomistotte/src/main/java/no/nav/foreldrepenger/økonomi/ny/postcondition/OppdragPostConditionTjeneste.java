package no.nav.foreldrepenger.økonomi.ny.postcondition;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomi.ny.domene.KjedeNøkkel;
import no.nav.foreldrepenger.økonomi.ny.domene.OppdragKjede;
import no.nav.foreldrepenger.økonomi.ny.domene.Ytelse;
import no.nav.foreldrepenger.økonomi.ny.domene.samlinger.GruppertYtelse;
import no.nav.foreldrepenger.økonomi.ny.mapper.EksisterendeOppdragMapper;
import no.nav.foreldrepenger.økonomi.ny.mapper.TilkjentYtelseMapper;
import no.nav.foreldrepenger.økonomi.ny.tjeneste.EndringsdatoTjeneste;
import no.nav.foreldrepenger.økonomi.ny.util.SetUtil;
import no.nav.foreldrepenger.økonomi.økonomistøtte.ØkonomioppdragRepository;
import no.nav.vedtak.util.env.Environment;

@ApplicationScoped
public class OppdragPostConditionTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(OppdragPostConditionTjeneste.class);

    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private ØkonomioppdragRepository økonomioppdragRepository;
    private FagsakRepository fagsakRepository;
    private FamilieHendelseRepository familieHendelseRepository;

    OppdragPostConditionTjeneste() {
        //for CDI proxy
    }

    @Inject
    public OppdragPostConditionTjeneste(BehandlingRepository behandlingRepository, BeregningsresultatRepository beregningsresultatRepository, ØkonomioppdragRepository økonomioppdragRepository, FagsakRepository fagsakRepository, FamilieHendelseRepository familieHendelseRepository) {
        this.behandlingRepository = behandlingRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.økonomioppdragRepository = økonomioppdragRepository;
        this.fagsakRepository = fagsakRepository;
        this.familieHendelseRepository = familieHendelseRepository;
    }

    public void softPostCondition(Long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        FagsakYtelseType fagsakYtelseType = behandling.getFagsakYtelseType();
        if (fagsakYtelseType == FagsakYtelseType.FORELDREPENGER || fagsakYtelseType == FagsakYtelseType.SVANGERSKAPSPENGER) {
            BeregningsresultatEntitet beregningsresultat = beregningsresultatRepository.hentUtbetBeregningsresultat(behandling.getId()).orElse(null);
            softPostCondition(behandling, beregningsresultat);
        }
    }

    private void softPostCondition(Behandling behandling, BeregningsresultatEntitet beregningsresultat) {
        try {
            sammenlignEffektAvOppdragMedTilkjentYtelse(behandling, beregningsresultat);
        } catch (Exception e) {
            logger.warn("Sammenligning av effekt av oppdrag mot tilkjent ytelse viser avvik for " + behandling.getFagsak().getSaksnummer() + " behandling " + behandling.getId() + ". Dette bør undersøkes og evt. patches: " + e.getMessage(), e);
        }
    }

    private void sammenlignEffektAvOppdragMedTilkjentYtelse(Behandling behandling, BeregningsresultatEntitet beregningsresultat) {
        Saksnummer saksnummer = behandling.getFagsak().getSaksnummer();
        List<Oppdragskontroll> oppdragene = økonomioppdragRepository.finnAlleOppdragForSak(saksnummer);
        Fagsak sak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElseThrow();
        Map<KjedeNøkkel, OppdragKjede> oppdragskjeder = EksisterendeOppdragMapper.tilKjeder(oppdragene);
        GruppertYtelse målbilde = TilkjentYtelseMapper.lagFor(sak.getYtelseType(), finnFamilieYtelseType(behandling)).fordelPåNøkler(beregningsresultat);

        for (KjedeNøkkel nøkkel : SetUtil.union(oppdragskjeder.keySet(), målbilde.getNøkler())) {
            OppdragKjede oppdragKjede = oppdragskjeder.getOrDefault(nøkkel, OppdragKjede.EMPTY);
            Ytelse ytelse = målbilde.getYtelsePrNøkkel().getOrDefault(nøkkel, Ytelse.EMPTY);
            Ytelse effektAvOppdragskjede = oppdragKjede.tilYtelse();

            validerLikhet(nøkkel, ytelse, effektAvOppdragskjede);
        }
    }

    private void validerLikhet(KjedeNøkkel nøkkel, Ytelse ytelse, Ytelse effektAvOppdragskjede) {
        LocalDate datoForDifferanse = EndringsdatoTjeneste.finnEndringsdato(ytelse, effektAvOppdragskjede);
        if (datoForDifferanse == null) {
            return;
        }
        if (Environment.current().isProd()) {
            throw OppdragValideringFeil.FACTORY.valideringsfeil("Forskjell i ny/gammel implementasjon fra dato " + datoForDifferanse).toException();
        } else {
            //nøkkel inneholder org.nr og skal ikke i loggen i prod
            throw OppdragValideringFeil.FACTORY.valideringsfeil("Forskjell i ny/gammel implementasjon for " + nøkkel + " fra dato " + datoForDifferanse +
                ". Gammel implementasjon ender med følgende effekt: " + effektAvOppdragskjede +
                ". Ønsket effekt fra tilkjent ytelse er : " + ytelse).toException();
        }
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
