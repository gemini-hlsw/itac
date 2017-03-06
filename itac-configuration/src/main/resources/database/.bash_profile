# .bash_profile

# Get the aliases and functions
if [ -f ~/.bashrc ]; then
    . ~/.bashrc
fi

# User specific environment and startup programs

PATH=$PATH:$HOME/bin

export PATH

# http://www.postgresql.org/docs/8.3/static/storage-file-layout.html
# The PGDATA directory contains several subdirectories and control files, as shown in Table 53-1. In addition to these required items, the cluster configuration files postgresql.conf, pg_hba.conf, and pg_ident.conf are traditionally stored in PGDATA (although in PostgreSQL 8.0 and later, it is possible to keep them elsewhere).

export PGDATA=/gemsoft/var/tac/pgsql/data
