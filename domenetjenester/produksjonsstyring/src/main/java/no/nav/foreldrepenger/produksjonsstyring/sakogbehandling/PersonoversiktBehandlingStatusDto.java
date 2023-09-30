package no.nav.foreldrepenger.produksjonsstyring.sakogbehandling;

import java.time.LocalDateTime;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Tema;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;

// OBS setter ikke feltet primaerBehandlingREF - etter diskusjon med SOB og Kvernstuen
// OBS applikasjonSakREF applikasjonBehandlingREF settes ikke - fordi de ikke var satt i MQ-tiden. Feedback fra SOB
public record PersonoversiktBehandlingStatusDto(String hendelseType,
                                                String hendelsesId,
                                                PersonoversiktKode hendelsesprodusentREF,
                                                LocalDateTime hendelsesTidspunkt,
                                                String behandlingsID,
                                                PersonoversiktKode behandlingstype,
                                                PersonoversiktKode sakstema,
                                                PersonoversiktKode behandlingstema,
                                                List<Aktoer> aktoerREF,
                                                String ansvarligEnhetREF,
                                                List<Ident> identREF,
                                                PersonoversiktKode avslutningsstatus) {

    public record Aktoer(String aktoerId) {}
    public record Ident(String ident) {}
    public record PersonoversiktKode(String value) {}

    public static PersonoversiktBehandlingStatusDto lagPersonoversiktBehandlingStatusDto(String hendelseType, String hendelseId,
                                                                                         Behandling behandling, LocalDateTime tidspunkt,
                                                                                         BehandlingTema behandlingTema, PersonIdent ident,
                                                                                         boolean avsluttet) {
        return new PersonoversiktBehandlingStatusDto(hendelseType,
            hendelseId,
            new PersonoversiktKode(Fagsystem.FPSAK.getOffisiellKode()),
            tidspunkt,
            String.format("%s_%s", Fagsystem.FPSAK.getOffisiellKode(), behandling.getId()),
            new PersonoversiktKode(behandling.getType().getOffisiellKode()),
            new PersonoversiktKode(Tema.FOR.getOffisiellKode()),
            new PersonoversiktKode(behandlingTema.getOffisiellKode()),
            List.of(new Aktoer(behandling.getAktørId().getId())),
            behandling.getBehandlendeEnhet(),
            ident != null ?  List.of(new Ident(ident.getIdent())) : List.of(),
            avsluttet ?  new PersonoversiktKode("ok") : null);
    }

    public static PersonoversiktBehandlingStatusDto lagPersonoversiktBehandlingStatusDto(String hendelseType, String hendelseId,
                                                                                         AktørId aktørId,
                                                                                         LocalDateTime tidspunkt,
                                                                                         BehandlingType behandlingType,
                                                                                         String behandlingRef,
                                                                                         BehandlingTema behandlingTema,
                                                                                         String behandlendeEnhet,
                                                                                         PersonIdent ident,
                                                                                         boolean avsluttet) {
        return new PersonoversiktBehandlingStatusDto(hendelseType,
            hendelseId,
            new PersonoversiktKode(Fagsystem.FPSAK.getOffisiellKode()),
            tidspunkt,
            behandlingRef,
            new PersonoversiktKode(behandlingType.getOffisiellKode()),
            new PersonoversiktKode(Tema.FOR.getOffisiellKode()),
            new PersonoversiktKode(behandlingTema.getOffisiellKode()),
            List.of(new Aktoer(aktørId.getId())),
            behandlendeEnhet,
            ident != null ?  List.of(new Ident(ident.getIdent())) : List.of(),
            avsluttet ?  new PersonoversiktKode("ok") : null);
    }

    @Override
    public String toString() {
        return "PersonoversiktBehandlingStatusDto{" + "hendelseType='" + hendelseType + '\'' + ", hendelsesTidspunkt=" + hendelsesTidspunkt
            + ", behandlingsID='" + behandlingsID + '\'' + '}';
    }

}
