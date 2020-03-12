package no.nav.foreldrepenger.domene.vedtak;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingOverlappInfotrygd;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingOverlappInfotrygdRepository;
import no.nav.foreldrepenger.domene.tid.ÅpenDatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.vedtak.infotrygd.rest.InfotrygdPSGrunnlag;
import no.nav.foreldrepenger.domene.vedtak.infotrygd.rest.InfotrygdSPGrunnlag;
import no.nav.vedtak.felles.integrasjon.aktør.klient.AktørConsumer;
import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.Grunnlag;
import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.Tema;
import no.nav.vedtak.felles.integrasjon.infotrygd.grunnlag.v1.respons.Vedtak;
import no.nav.vedtak.konfig.Tid;
/*
Når Foreldrepenger-sak, enten førstegang eller revurdering, innvilges sjekker vi for overlapp med pleiepenger eller sykepenger i Infortrygd på personen.
Overlapp er tilstede dersom noen av de vedtatte periodende i Infotrygd overlapper med noen av utbetalingsperiodene på iverksatt foreldrepenge-behandling
Ved overlapp lagres informasjonen til databasetabellen BEHANDLING_OVERLAPP_INFOTRYGD
Det er manuell håndtering av funnene videre.
Håndtering av overlapp av Foreldrepenger-foreldrepenger håndteres av klassen VurderOpphørAvYtelser som trigges av en prosesstask.
 */
@ApplicationScoped
public class IdentifiserOverlappendeInfotrygdYtelseTjeneste {
    private BeregningsresultatRepository beregningsresultatRepository;
    private AktørConsumer aktørConsumer;
    private InfotrygdPSGrunnlag infotrygdPSGrTjeneste;
    private InfotrygdSPGrunnlag infotrygdSPGrTjeneste;
    private BehandlingOverlappInfotrygdRepository overlappRepository;
    private static final Logger log = LoggerFactory.getLogger(IdentifiserOverlappendeInfotrygdYtelseTjeneste.class);


    IdentifiserOverlappendeInfotrygdYtelseTjeneste() {
        // for CDI
    }

    @Inject
    public IdentifiserOverlappendeInfotrygdYtelseTjeneste(BeregningsresultatRepository beregningsresultatRepository,
                                                          AktørConsumer aktørConsumer,
                                                          InfotrygdPSGrunnlag infotrygdPSGrTjeneste,
                                                          InfotrygdSPGrunnlag infotrygdSPGrTjeneste,
                                                          BehandlingOverlappInfotrygdRepository overlappRepository) {
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.aktørConsumer = aktørConsumer;
        this.infotrygdPSGrTjeneste = infotrygdPSGrTjeneste;
        this.infotrygdSPGrTjeneste = infotrygdSPGrTjeneste;
        this.overlappRepository = overlappRepository;
    }

    public void vurderOglagreEventueltOverlapp(Behandling behandling) {
        try {
            List<BehandlingOverlappInfotrygd> listeMedOverlapp = this.vurderOmOverlappInfotrygd(behandling);
                listeMedOverlapp.forEach(behandlingOverlappInfotrygd -> overlappRepository.lagre(behandlingOverlappInfotrygd));
        }
        catch (Exception e) {
            log.info("Identifisering av overlapp i Infotrygd feilet ", e);
        }
    }

    public List<BehandlingOverlappInfotrygd> vurderOmOverlappInfotrygd(Behandling behandling) {
        LocalDate førsteUttaksDatoFP = finnFørsteUttaksDato(behandling.getId());
        //Henter alle utbetalingsperioder på behandling som er iverksatt
        List<ÅpenDatoIntervallEntitet> perioderFp = hentPerioderFp(behandling.getId());
        //sjekker om noen av vedtaksperiodene i Infotrygd på sykepenger eller pleiepenger overlapper med perioderFp
        List<BehandlingOverlappInfotrygd> overlapperIT  = harFPYtelserSomOverlapperIT (behandling, førsteUttaksDatoFP, perioderFp);

        return overlapperIT;
    }

    public List<BehandlingOverlappInfotrygd> harFPYtelserSomOverlapperIT (Behandling behandling, LocalDate førsteUttaksdatoFp, List<ÅpenDatoIntervallEntitet> perioderFP) {

        List<Grunnlag> infotrygdGrunnlaglist = hentPSogSPGrunnlag(behandling.getAktørId(), førsteUttaksdatoFp);

        if (infotrygdGrunnlaglist.isEmpty()) {
            return Collections.emptyList();
        }

        List<BehandlingOverlappInfotrygd> overlappene = new ArrayList<>();
        infotrygdGrunnlaglist.forEach( grunnlag -> {
            grunnlag.getVedtak().stream()
                .forEach(vedtak -> {
                    perioderFP.stream()
                        .filter(p -> p.overlapper(ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(vedtak.getPeriode().getFom(), vedtak.getPeriode().getTom())))
                        .map(p -> opprettOverlappIT (behandling, grunnlag.getTema(), vedtak, p))
                        .forEach(behandlingOverlappInfotrygd -> overlappene.add(behandlingOverlappInfotrygd));
                });
        });
        return overlappene;
    }

    private List<Grunnlag> hentPSogSPGrunnlag (AktørId aktørId, LocalDate førsteUttaksdatoFp) {
        var ident = getFnrFraAktørId(aktørId);

        List<Grunnlag> infotrygdPSGrunnlag = infotrygdPSGrTjeneste.hentGrunnlag(ident.getIdent(), førsteUttaksdatoFp.minusWeeks(1), førsteUttaksdatoFp.plusYears(3));
        List<Grunnlag> infotrygdSPGrunnlag = infotrygdSPGrTjeneste.hentGrunnlag(ident.getIdent(), førsteUttaksdatoFp.minusWeeks(1), førsteUttaksdatoFp.plusYears(3));

        List<Grunnlag> infotrygdGrunnlag = merge(infotrygdPSGrunnlag, infotrygdSPGrunnlag);

        return infotrygdGrunnlag;
    }

    // Generisk funksjon for å legge sammen 2 lister
    private static<T> List<T> merge(List<T> list1, List<T> list2)
    {
        return Stream.of(list1, list2)
            .flatMap(x -> x.stream())
            .collect(Collectors.toList());
    }

    private PersonIdent getFnrFraAktørId(AktørId aktørId) {
        return aktørConsumer.hentPersonIdentForAktørId(aktørId.getId()).map(PersonIdent::new).orElseThrow();
    }

    private List<ÅpenDatoIntervallEntitet> hentPerioderFp(Long behandlingId) {
        Optional<BeregningsresultatEntitet> berResultat = beregningsresultatRepository.hentBeregningsresultat(behandlingId);

         return berResultat.map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
            .filter(beregningsresultatPeriode -> beregningsresultatPeriode.getDagsats() > 0)
            .map(p-> ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(p.getBeregningsresultatPeriodeFom(), p.getBeregningsresultatPeriodeTom()))
             .collect(Collectors.toList());
    }

    private LocalDate finnFørsteUttaksDato(Long behandlingId) {
        Optional<BeregningsresultatEntitet> berResultat = beregningsresultatRepository.hentBeregningsresultat(behandlingId);
        Optional<LocalDate> minFom = berResultat.map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
            .filter(beregningsresultatPeriode -> beregningsresultatPeriode.getDagsats() > 0)
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)
            .min(Comparator.naturalOrder());
        return minFom.orElse(Tid.TIDENES_ENDE);
    }

    private BehandlingOverlappInfotrygd opprettOverlappIT(Behandling behandling, Tema tema, Vedtak vedtak, ÅpenDatoIntervallEntitet periode) {
        ÅpenDatoIntervallEntitet periodeInfotrygd = ÅpenDatoIntervallEntitet.fraOgMedTilOgMed(vedtak.getPeriode().getFom(), vedtak.getPeriode().getTom());
        BehandlingOverlappInfotrygd behandlingOverlappInfotrygd = BehandlingOverlappInfotrygd.builder()
            .medSaksnummer(behandling.getFagsak().getSaksnummer())
            .medBehandlingId(behandling.getId())
            .medPeriodeInfotrygd(periodeInfotrygd)
            .medPeriodeVL(periode)
            .medYtelseInfotrygd(tema.getKode().name())
            .build();
        return behandlingOverlappInfotrygd;
    }
}
