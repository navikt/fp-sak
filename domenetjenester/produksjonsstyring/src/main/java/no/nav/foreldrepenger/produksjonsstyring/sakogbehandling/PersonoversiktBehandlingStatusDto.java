package no.nav.foreldrepenger.produksjonsstyring.sakogbehandling;

import java.time.LocalDateTime;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.Tema;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.domene.typer.PersonIdent;

public abstract class PersonoversiktBehandlingStatusDto {

    private String hendelseType;
    private String hendelsesId;
    private PersonoversiktKode hendelsesprodusentREF;
    private LocalDateTime hendelsesTidspunkt;
    private String behandlingsID;
    private PersonoversiktKode behandlingstype;
    private PersonoversiktKode sakstema;
    private PersonoversiktKode behandlingstema;
    private List<Aktoer> aktoerREF;
    private String ansvarligEnhetREF;
    private List<Ident> identREF;

    protected PersonoversiktBehandlingStatusDto(String hendelseType, String hendelseId, BehandlingStatusDto behandling, PersonIdent ident) {
        this.hendelseType = hendelseType;
        this.hendelsesTidspunkt = behandling.getHendelsesTidspunkt();
        this.hendelsesId = hendelseId;
        this.ansvarligEnhetREF = behandling.getEnhet().enhetId();
        this.hendelsesprodusentREF = new PersonoversiktKode(Fagsystem.FPSAK.getOffisiellKode());
        this.behandlingsID = String.format("%s_%s", Fagsystem.FPSAK.getOffisiellKode(), behandling.getBehandlingId());
        this.behandlingstype = new PersonoversiktKode(behandling.getBehandlingType().getOffisiellKode());
        this.sakstema = new PersonoversiktKode(Tema.FOR.getOffisiellKode());
        this.behandlingstema = new PersonoversiktKode(behandling.getBehandlingTema().getOffisiellKode());
        this.aktoerREF = List.of(new Aktoer(behandling.getAktørId().getId()));
        this.identREF = ident != null ?  List.of(new Ident(ident.getIdent())) : List.of();
    }

    @Override
    public String toString() {
        return "PersonoversiktBehandlingStatusDto{" + "hendelseType='" + hendelseType + '\'' + ", hendelsesTidspunkt=" + hendelsesTidspunkt
            + ", behandlingsID='" + behandlingsID + '\'' + '}';
    }

    private record Aktoer(String aktoerId) {}
    private record Ident(String ident) {}

    private record PersonoversiktKode(String value) {}

    public static class PersonoversiktBehandlingOpprettetDto extends PersonoversiktBehandlingStatusDto {

        public PersonoversiktBehandlingOpprettetDto(String hendelseId, BehandlingStatusDto behandling, PersonIdent ident) {
            super("behandlingOpprettet", hendelseId, behandling, ident);
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }

    public static class PersonoversiktBehandlingAvsluttetDto extends PersonoversiktBehandlingStatusDto {

        private PersonoversiktKode avslutningsstatus;

        public PersonoversiktBehandlingAvsluttetDto(String hendelseId, BehandlingStatusDto behandling, PersonIdent ident) {
            super("behandlingAvsluttet", hendelseId, behandling, ident);
            this.avslutningsstatus = new PersonoversiktKode("ok");
        }

        @Override
        public String toString() {
            return super.toString();
        }

    }
}
