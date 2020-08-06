package no.nav.foreldrepenger.økonomi.økonomistøtte.kontantytelse.es.wrapper;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandling.es.UtledVedtakResultatTypeES;
import no.nav.foreldrepenger.behandling.impl.FinnAnsvarligSaksbehandler;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

public class OppdragInputES {
    private final Saksnummer saksnummer;
    private final Behandling behandling;
    private final BehandlingVedtak behVedtak;
    private final PersonIdent personIdent;
    private final String kodeKlassifik;
    private final Optional<ForrigeOppdragInputES> tidligereBehandlingInfo;
    private final long sats;

    public OppdragInputES(Saksnummer saksnummer, Behandling behandling,
                          BehandlingVedtak behVedtak,
                          PersonIdent personIdent,
                          String kodeKlassifik,
                          long sats,
                          Optional<ForrigeOppdragInputES> tidligereBehandlingInfo) {
        this.saksnummer = saksnummer;
        this.behandling = behandling;
        this.behVedtak = behVedtak;
        this.personIdent = personIdent;
        this.kodeKlassifik = kodeKlassifik;
        this.sats = sats;
        this.tidligereBehandlingInfo = tidligereBehandlingInfo;
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public Behandling getBehandling() {
        return behandling;
    }

    public Optional<BehandlingVedtak> getBehVedtak() {
        return Optional.ofNullable(behVedtak);
    }

    public String getAnsvarligSaksbehandler() {
        return getBehVedtak().map(BehandlingVedtak::getAnsvarligSaksbehandler).orElse(FinnAnsvarligSaksbehandler.finn(behandling));
    }

    public LocalDate getVedtaksdato() {
        return getBehVedtak().map(BehandlingVedtak::getVedtaksdato).orElse(LocalDate.now());
    }

    public PersonIdent getPersonIdent() {
        return personIdent;
    }

    public VedtakResultatType getVedtakResultatType() {
        return getBehVedtak().map(BehandlingVedtak::getVedtakResultatType).orElse(finnVedtakResultatTypeFraBehandling());
    }

    private VedtakResultatType finnVedtakResultatTypeFraBehandling() {
        BehandlingResultatType behandlingResultatType = utledBehandlingsresultatType(behandling.getBehandlingsresultat());
        Objects.requireNonNull(behandling, "behandling");
        Objects.requireNonNull(behandlingResultatType);

        return UtledVedtakResultatTypeES.utled(behandling.getType(), behandlingResultatType);
    }

    private BehandlingResultatType utledBehandlingsresultatType(Behandlingsresultat behandlingsresultat) {
        Objects.requireNonNull(behandlingsresultat, "behandlingsresultat"); //NOSONAR
        return behandlingsresultat.isVilkårAvslått() ? BehandlingResultatType.AVSLÅTT : BehandlingResultatType.INNVILGET;
    }

    public String getKodeKlassifik() {
        return kodeKlassifik;
    }

    public Optional<Oppdrag110> getForrigeOppddragForSak() {
        return tidligereBehandlingInfo.map(ForrigeOppdragInputES::getForrigeOppddragForSak);
    }

    private ForrigeOppdragInputES getTidligereBehandlingInfo() {
        return tidligereBehandlingInfo.orElseThrow(() -> new IllegalStateException("Fant ikke TidligereBehandlingInfoES for behandling " + behandling.getId()));
    }

    public BehandlingVedtak getTidligereVedtak() {
        return getTidligereBehandlingInfo().getTidligereVedtak();
    }

    public long getSats() {
        return sats;
    }

    public long getSatsFraTidligereBehandling() {
        return getTidligereBehandlingInfo().getSats();
    }
}
