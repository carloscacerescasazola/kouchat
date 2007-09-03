
/***************************************************************************
 *   Copyright 2006-2007 by Christian Ihle                                 *
 *   kontakt@usikkert.net                                                  *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 ***************************************************************************/

package kouchat.misc;

public class Nick implements Comparable<Nick>
{
	private String nick, awayMsg, ipAddress;
	private int code;
	private long lastIdle;
	private boolean writing, away, me;
	
	public Nick( String nick, int code )
	{
		this.nick = nick;
		this.code = code;
		lastIdle = 0;
		awayMsg = "";
		writing = false;
		away = false;
		ipAddress = "";
		me = false;
	}
	
	public boolean isMe()
	{
		return me;
	}

	public void setMe( boolean me )
	{
		this.me = me;
	}

	public int getCode()
	{
		return code;
	}
	
	public void setCode( int code )
	{
		this.code = code;
	}
	
	public String getNick()
	{
		return nick;
	}
	
	public void setNick( String nick )
	{
		this.nick = nick;
	}
	
	public long getLastIdle()
	{
		return lastIdle;
	}
	
	public void setLastIdle( long lastIdle )
	{
		this.lastIdle = lastIdle;
	}

	public boolean isAway()
	{
		return away;
	}

	public void setAway( boolean away )
	{
		this.away = away;
	}

	public String getAwayMsg()
	{
		return awayMsg;
	}

	public void setAwayMsg( String awayMsg )
	{
		this.awayMsg = awayMsg;
	}

	public boolean isWriting()
	{
		return writing;
	}

	public void setWriting( boolean writing )
	{
		this.writing = writing;
	}

	public String getIpAddress()
	{
		return ipAddress;
	}

	public void setIpAddress( String ipAddress )
	{
		this.ipAddress = ipAddress;
	}

	public String toString()
	{
		return nick;
	}
	
	@Override
	public int compareTo( Nick compNick )
	{
		return nick.compareToIgnoreCase( compNick.getNick() );
	}
}
